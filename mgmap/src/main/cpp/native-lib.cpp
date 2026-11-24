#include <jni.h>
#include <cstdlib>
#include <android/log.h>

#define LOG_TAG "NativeBuffer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jobject Java_mg_mgmap_application_util_NativeBuffer_allocateDirectBuffer(JNIEnv* env, jobject thiz, jlong size) {
    void* nativeBuffer = std::malloc(size);
    if (nativeBuffer == nullptr) {
        LOGI("Failed to allocate %lld bytes of native memory", size);
        return nullptr;
    }
    return env->NewDirectByteBuffer(nativeBuffer, size);
}

extern "C" JNIEXPORT void Java_mg_mgmap_application_util_NativeBuffer_freeDirectBuffer(JNIEnv* env, jobject thiz, jobject directBuffer) {
    void* ptr = env->GetDirectBufferAddress(directBuffer);
    if (ptr != nullptr) {
        std::free(ptr);
//        LOGI("Native memory freed");
    }
}
