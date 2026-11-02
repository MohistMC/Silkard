package com.mohistmc.silkard.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Kernel32 extends Library {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

    Pointer GetCurrentProcess();

    boolean SetProcessWorkingSetSize(Pointer hProcess, int dwMinimumWorkingSetSize, int dwMaximumWorkingSetSize);
}
