package cn.langya;

import com.sun.jna.*;
import com.sun.jna.ptr.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LangYa466
 * @since 2025/1/20
 */
public class WindowsUtil {

    // JNA接口定义
    public interface Kernel32 extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        int MEM_COMMIT = 0x00001000;
        int MEM_RESERVE = 0x00002000;
        int PAGE_READWRITE = 0x04;
        int PROCESS_ALL_ACCESS = 0x1F0FFF; // 权限

        boolean ReadProcessMemory(Pointer hProcess, Pointer lpBaseAddress, Pointer lpBuffer, int dwSize, IntByReference lpNumberOfBytesRead);
        boolean WriteProcessMemory(Pointer hProcess, Pointer lpBaseAddress, Pointer lpBuffer, int nSize, IntByReference lpNumberOfBytesWritten);
        Pointer VirtualAllocEx(Pointer hProcess, Pointer lpAddress, int dwSize, int flAllocationType, int flProtect);
        boolean VirtualFreeEx(Pointer hProcess, Pointer lpAddress, int dwSize, int dwFreeType);
        void CloseHandle(Pointer hObject);
        int GetLastError();
        boolean EnumProcessModules(Pointer hProcess, PointerByReference lphModule, int cb, IntByReference lpcbNeeded);
        Pointer GetProcAddress(Pointer hModule, String lpProcName);
        Pointer GetModuleHandle(String lpModuleName);
        Pointer CreateRemoteThread(Pointer hProcess, Pointer lpThreadAttributes, int dwStackSize, Pointer lpStartAddress, Pointer lpParameter, int dwCreationFlags, PointerByReference lpThreadId);
        void WaitForSingleObject(Pointer hHandle, int dwMilliseconds);

        // 添加 OpenProcess 方法定义
        Pointer OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);
    }

    // Method to read memory from a process
    public static byte[] readMemory(Pointer hProcess, Pointer address, int size) {
        byte[] buffer = new byte[size];
        Memory memory = new Memory(size);  // 使用 Memory 类型
        IntByReference bytesRead = new IntByReference();
        boolean success = Kernel32.INSTANCE.ReadProcessMemory(hProcess, address, memory, size, bytesRead);
        if (!success) {
            throw new RuntimeException("Failed to read memory: " + Kernel32.INSTANCE.GetLastError());
        }
        memory.read(0, buffer, 0, size);  // 将 Memory 中的数据读回 byte[]
        return buffer;
    }

    // Method to write memory to a process
    public static void writeMemory(Pointer hProcess, Pointer address, byte[] data) {
        Memory memory = new Memory(data.length);
        memory.write(0, data, 0, data.length);  // 将字节数组写入 Memory
        IntByReference bytesWritten = new IntByReference();
        boolean success = Kernel32.INSTANCE.WriteProcessMemory(hProcess, address, memory, data.length, bytesWritten);
        if (!success) {
            throw new RuntimeException("Failed to write memory: " + Kernel32.INSTANCE.GetLastError());
        }
    }

    // Method to allocate memory in a process
    public static Pointer allocMemory(Pointer hProcess, int size) {
        Pointer memory = Kernel32.INSTANCE.VirtualAllocEx(hProcess, null, size, Kernel32.MEM_COMMIT | Kernel32.MEM_RESERVE, Kernel32.PAGE_READWRITE);
        if (memory == null) {
            throw new RuntimeException("Failed to allocate memory: " + Kernel32.INSTANCE.GetLastError());
        }
        return memory;
    }

    // Method to free memory in a process
    public static void freeMemory(Pointer hProcess, Pointer memory) {
        boolean success = Kernel32.INSTANCE.VirtualFreeEx(hProcess, memory, 0, 0x8000); // MEM_RELEASE
        if (!success) {
            throw new RuntimeException("Failed to free memory: " + Kernel32.INSTANCE.GetLastError());
        }
    }

    // Method to get a list of modules in a process
    public static List<Pointer> getModuleList(Pointer hProcess) {
        List<Pointer> moduleList = new ArrayList<>();
        PointerByReference modules = new PointerByReference();
        IntByReference cbNeeded = new IntByReference();

        boolean success = Kernel32.INSTANCE.EnumProcessModules(hProcess, modules, 0, cbNeeded);
        if (!success) {
            throw new RuntimeException("Failed to enumerate modules: " + Kernel32.INSTANCE.GetLastError());
        }

        // 获取模块数目
        int moduleCount = cbNeeded.getValue() / Native.POINTER_SIZE;
        for (int i = 0; i < moduleCount; i++) {
            Pointer modulePointer = modules.getValue().share((long) i * Native.POINTER_SIZE);
            moduleList.add(modulePointer);
        }

        return moduleList;
    }

    // Method to inject a DLL into a process
    public static boolean injectDll(Pointer hProcess, String dllPath) {
        Pointer memory = allocMemory(hProcess, dllPath.length() + 1);
        writeMemory(hProcess, memory, dllPath.getBytes());

        // 获取 LoadLibraryA 函数地址
        Pointer loadLibraryAddr = getProcAddress("kernel32", "LoadLibraryA");
        if (loadLibraryAddr == null) {
            throw new RuntimeException("Failed to find LoadLibraryA address.");
        }

        // 创建远程线程来加载 DLL
        Pointer thread = Kernel32.INSTANCE.CreateRemoteThread(hProcess, null, 0, loadLibraryAddr, memory, 0, null);
        if (thread == null) {
            throw new RuntimeException("Failed to create remote thread: " + Kernel32.INSTANCE.GetLastError());
        }

        Kernel32.INSTANCE.WaitForSingleObject(thread, 0xFFFFFFFF); // 等待线程结束

        // 清理
        freeMemory(hProcess, memory);
        Kernel32.INSTANCE.CloseHandle(thread);

        return true;
    }

    // Utility method to get the address of a function from a module
    private static Pointer getProcAddress(String moduleName, String functionName) {
        Pointer hModule = Kernel32.INSTANCE.GetModuleHandle(moduleName);
        if (hModule == null) {
            throw new RuntimeException("Failed to get module handle for: " + moduleName);
        }
        return Kernel32.INSTANCE.GetProcAddress(hModule, functionName);
    }
}
