package com.mohistmc.silkard.util;

import com.sun.jna.Pointer;

public class MemoryUtils {

    public static String setProcessWorkingSetSize(int minSizeMB, int maxSizeMB) {
        Pointer processHandle = Kernel32.INSTANCE.GetCurrentProcess();
        boolean success = Kernel32.INSTANCE.SetProcessWorkingSetSize(processHandle, minSizeMB, maxSizeMB);
        return !success ? I18n.as("memoryutils.failed") : I18n.as("memoryutils.success");
    }
}
