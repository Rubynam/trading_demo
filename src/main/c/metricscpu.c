#include <jni.h>
#include <sys/types.h>
#include <sys/sysctl.h>
#include <stdio.h>
#include <pthread.h>         // Để sử dụng pthread API
#include <mach/mach.h>       // Để làm việc với Mach threads
#include <mach/thread_policy.h>


JNIEXPORT jstring JNICALL Java_org_trading_insfrastructure_monitor_CoreInfo_getCurrentCpu(JNIEnv *env, jobject obj) {
    uint64_t core_id;

        // ARM64 Assembly: Read TPIDR_EL0 (Thread ID Register)
    __asm__ volatile ("mrs %0, TPIDR_EL0" : "=r" (core_id));
    char result[32];
    snprintf(result, sizeof(result), "%llu", core_id);

    return (*env)->NewStringUTF(env, result);


}
