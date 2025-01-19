package cn.langya;

import com.sun.jna.*;

import java.util.List;

/**
 * @author LangYa466
 * @since 2025/1/20
 */
public class Main {

    public static void main(String[] args) {
        // 测试用例：打开进程
        int processId = 1234; // 进程ID，需要替换为实际的目标进程ID
        Pointer hProcess = WindowsUtil.Kernel32.INSTANCE.OpenProcess(WindowsUtil.Kernel32.PROCESS_ALL_ACCESS, false, processId);
        if (hProcess == null) {
            System.err.println("Failed to open process: " + WindowsUtil.Kernel32.INSTANCE.GetLastError());
            return;
        }

        // 测试用例：读取内存
        try {
            Pointer address = new Pointer(0x00000000); // 替换为目标内存地址
            byte[] data = WindowsUtil.readMemory(hProcess, address, 1024); // 假设我们读取1024字节
            System.out.println("Read memory: " + new String(data));
        } catch (Exception e) {
            System.err.println("Error reading memory: " + e.getMessage());
        }

        // 测试用例：写入内存
        try {
            byte[] dataToWrite = "Hello from Java!".getBytes();
            Pointer addressToWrite = new Pointer(0x00000000); // 替换为目标内存地址
            WindowsUtil.writeMemory(hProcess, addressToWrite, dataToWrite);
            System.out.println("Memory written successfully.");
        } catch (Exception e) {
            System.err.println("Error writing memory: " + e.getMessage());
        }

        // 测试用例：分配内存
        try {
            Pointer allocatedMemory = WindowsUtil.allocMemory(hProcess, 1024); // 分配1024字节
            System.out.println("Memory allocated at: " + allocatedMemory);
            WindowsUtil.freeMemory(hProcess, allocatedMemory);
            System.out.println("Memory freed successfully.");
        } catch (Exception e) {
            System.err.println("Error allocating or freeing memory: " + e.getMessage());
        }

        // 测试用例：获取模块列表
        try {
            List<Pointer> modules = WindowsUtil.getModuleList(hProcess);
            System.out.println("Modules in process:");
            for (Pointer module : modules) {
                System.out.println("Module: " + module);
            }
        } catch (Exception e) {
            System.err.println("Error enumerating modules: " + e.getMessage());
        }

        // 测试用例：注入 DLL
        try {
            String dllPath = "C:\\path\\to\\your\\dll.dll"; // 替换为 DLL 的路径
            boolean injected = WindowsUtil.injectDll(hProcess, dllPath);
            if (injected) {
                System.out.println("DLL injected successfully.");
            } else {
                System.out.println("DLL injection failed.");
            }
        } catch (Exception e) {
            System.err.println("Error injecting DLL: " + e.getMessage());
        }

        // 关闭进程句柄
        WindowsUtil.Kernel32.INSTANCE.CloseHandle(hProcess);
    }
}
