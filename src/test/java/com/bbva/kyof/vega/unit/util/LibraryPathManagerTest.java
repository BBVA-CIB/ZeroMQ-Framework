package com.bbva.kyof.vega.unit.util;

import com.bbva.kyof.vega.exception.LLZException;
import com.bbva.kyof.vega.util.LibraryPathManager;
import com.bbva.kyof.vega.util.OSManager;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created to test error where bad paths are passed
 *
 * Created by XE48745 on 17/09/2015.
 */
public class LibraryPathManagerTest
{
    /**
     * The LibraryPathManager is singletone but in order to test it we need to create new instances
     */
    private LibraryPathManager createManagerInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Constructor<?>[] cons = LibraryPathManager.class.getDeclaredConstructors();
        cons[0].setAccessible(true);
        return (LibraryPathManager)cons[0].newInstance((Object[]) null);
    }

    @Test(expected = LLZException.class)
    public void testLoadNativeLibrariesFailPathNotExist() throws java.lang.Exception
    {
        final LibraryPathManager libraryPathManager = this.createManagerInstance();
        libraryPathManager.loadNativeLibraries("lskdjflskjf");
    }

    @Test(expected = LLZException.class)
    public void testLoadNativeLibrariesFailNotDirectory() throws java.lang.Exception
    {
        final LibraryPathManager libraryPathManager = this.createManagerInstance();
        final String path = LibraryPathManagerTest.class.getClassLoader().getResource("config/validConfiguration.xml").getPath();
        libraryPathManager.loadNativeLibraries(path);
    }

    @Test
    public void testLoadMultipath() throws java.lang.Exception
    {
        final LibraryPathManager libraryPathManager = this.createManagerInstance();

        // Path 1
        final String path1String = "libpathmanager" + File.separator + "path1" + File.separator + "file.txt";
        final String path1 = LibraryPathManagerTest.class.getClassLoader().getResource(path1String).getPath();
        final String path1Dir = new File(path1).getParentFile().getAbsolutePath();

        // Path 2
        final String path2String = "libpathmanager" + File.separator + "path2" + File.separator + "file.txt";
        final String path2 = LibraryPathManagerTest.class.getClassLoader().getResource(path2String).getPath();
        final String path2Dir = new File(path2).getParentFile().getAbsolutePath();

        // Join paths
        final String fullMultipath = path1Dir + OSManager.getInstance().getLibraryPathSeparator() + path2Dir;

        libraryPathManager.loadNativeLibraries(fullMultipath);
    }
}