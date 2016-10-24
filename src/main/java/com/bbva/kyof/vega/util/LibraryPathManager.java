package com.bbva.kyof.vega.util;

import com.bbva.kyof.vega.exception.LLZException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQException;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class will try to automatically set the library path if the right environmental variables are
 * settled or if it is settled programatically.
 *
 * If already settled it wont be reloaded even if new managers are fired
 */
public final class LibraryPathManager
{
    /** Instance of a Logger class*/
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryPathManager.class);

    /** Singletone instance of the manager */
    private static final LibraryPathManager INSTANCE = new LibraryPathManager();

    /** Library path property name */
    private static final String JAVA_LIBRARY_PATH_PROP = "java.library.path";

    /** Lock to avoid concurrent libraries load */
    private final Object lock = new Object();

    /** True if the libraries have been already loaded */
    private boolean librariesLoaded = false;

    /**Instance of OSManager*/
    private final OSManager osManager = new OSManager();

    /** Private constructor to avoid instantiation */
    private LibraryPathManager()
    {
        // Nothing to do here
    }

    /** @return the singletone instance of the manager */
    public static LibraryPathManager getInstance()
    {
        return INSTANCE;
    }

    /**
     * Load the native libraries into the classpath.
     *
     * It will first look for a programaticaly settled path
     *
     * @param nativeLibsPath path to the native libraries
     * @throws ZMQException exception thrown if there is a problem loading the libraries
     */
    public void loadNativeLibraries(final String nativeLibsPath) throws LLZException
    {
        synchronized (this.lock)
        {
            // If already loaded don't try again
            if (this.librariesLoaded)
            {
                return;
            }

            // If settled programatically load the libraries
            if (nativeLibsPath != null && !nativeLibsPath.isEmpty())
            {
                LOGGER.info("ZMQ native libraries path settled programmatically to folder [{}]", nativeLibsPath);

                this.splitAndLoadNativeLibraries(nativeLibsPath);
            }
            else
            {
                LOGGER.info("ZMQ native libraries path not settled programmatically, will use default value [{}]", System.getProperty(JAVA_LIBRARY_PATH_PROP));
            }

            this.librariesLoaded = true;
        }
    }

    /**
     * Load the native libraries specified in the given path
     *
     * @param path the path with the native libraries, it may contain several directories separated by ':' or ';'
     * @throws LLZException exception thrown if there is any problem loading the libraries
     */
    private void splitAndLoadNativeLibraries(final String path) throws LLZException
    {
        final List<String> splitedPath = this.splitPath(path);

        // Check if the given path is a valid path
        this.validateZmqLibraryPath(splitedPath);

        // Get the original library path and make sure the given path is not already included
        final List<String> originalPaths = this.splitPath(System.getProperty(JAVA_LIBRARY_PATH_PROP));

        // Remove already existing paths already settled
        this.removeExistingPaths(splitedPath, originalPaths);

        // Create the new path
        splitedPath.addAll(originalPaths);
        final String newPath = this.createPath(splitedPath);

        LOGGER.info("New library Path for the Application [{}]", newPath);

        // Set the new path
        System.setProperty(JAVA_LIBRARY_PATH_PROP, newPath);

        try
        {
            // This step is required to force Java to reload the path that is cached when the JVM is started
            final Field fieldSysPath = ClassLoader.class.getDeclaredField( "sys_paths" );
            fieldSysPath.setAccessible( true );
            fieldSysPath.set( null, null );
        }
        catch (final NoSuchFieldException e)
        {
            LOGGER.error("Error forcing the reload of the library path, sys_paths not found", e);
            throw new LLZException("Error reloading the library path in the JVM, sys_paths not found", e);
        }
        catch (final IllegalAccessException e)
        {
            LOGGER.error("Error forcing the reload of the library path, cannot modify sys_paths", e);
            throw new LLZException("Error reloading the library path in the JVM, cannot modify sys_paths", e);
        }
    }

    /**
     * Create a full path for the java library path given several paths, it will use the right separator based on the OS
     * 
     * @param splitPath split path with all directories to join in a single path
     * @return the concatenated path
     */
    private String createPath(final List<String> splitPath)
    {
        final String pathSeparator = osManager.getLibraryPathSeparator();

        final StringBuilder result = new StringBuilder();

        for(String path : splitPath)
        {
            result.append(path);
            result.append(pathSeparator);
        }

        return result.toString();
    }

    /**
     * Take the splitPath parameter and remove any entry that already exists in the originalPaths parameter
     *
     * @param splitPath zmq library paths already split
     * @param originalPaths original library paths already split
     */
    private void removeExistingPaths(final List<String> splitPath, final List<String> originalPaths)
    {
        final Iterator<String> iterator = splitPath.iterator();

        while(iterator.hasNext())
        {
            final String current = iterator.next();

            if (originalPaths.contains(current))
            {
                iterator.remove();
                LOGGER.warn("The library path {} has already been settled using Java JVM launch parameters", current);
            }
        }
    }

    /**
     * Validate the given list of paths checking if they exists, is a directory and it is not empty
     *
     * @param inputPaths paths to validate
     */
    private void validateZmqLibraryPath(final List<String> inputPaths) throws LLZException
    {
        for(final String path : inputPaths)
        {
            // First check if the path exists and if it a directory
            final File pathDirectory = new File(path);

            if (!pathDirectory.exists())
            {
                LOGGER.error("ZMQ native libraries path {} does not exists", path);
                throw new LLZException("ZMQ native libraries path does not exists");
            }
            else if (!pathDirectory.isDirectory())
            {
                LOGGER.error("ZMQ native libraries path {} is not a directory", path);
                throw new LLZException("ZMQ native libraries path is not a directory.");
            }
            else
            {
                final File [] files =  pathDirectory.listFiles();

                if (files != null && files.length == 0 )
                {
                    LOGGER.error("ZMQ native libraries path {} is empty", path);
                    throw new LLZException("ZMQ native libraries path is empty.");
                }
            }
        }
    }

    /**
     * Split the path based on the operative system path separator
     * @param path the path to split
     * @return the splitted path
     */
    private List<String> splitPath(final String path)
    {
        final List<String> result = new LinkedList<>();

        if (path == null)
        {
            return result;
        }

        // Split the path
        String[] splitted;
        switch (osManager.getOperatingSystemType())
        {
            case WINDOWS:
                splitted = path.split(OSManager.WIN_PATH_SEP);
                break;
            case LINUX:
            case MACOS:
            default:
                splitted = path.split(OSManager.LINUX_PATH_SEP);
                break;
        }

        // Trim the results
        for (final String pathPart : splitted)
        {
            final String trimmedPath = pathPart.trim();

            if (!trimmedPath.isEmpty())
            {
                result.add(trimmedPath);
            }
        }

        return result;
    }
}
