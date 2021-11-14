#include <limits.h>
#include <jni.h>
#include <stdlib.h>
#include <syslog.h>
#include <pthread.h>
#include <include/magic.h>
#include <include/archive.h>
#include <include/archive_entry.h>

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>


magic_t magic_cookie;
jobject asset_g_ref;

JNIEXPORT jint JNICALL
Java_com_karthek_android_s_files2_helpers_FileType_c_1magic_1open(JNIEnv *env,
                                                                  __unused jclass clazz,
                                                                  jobject asset_manager) {
    magic_cookie = magic_open(MAGIC_MIME_TYPE);
    //magic_load(magic, "/data/user/0/com.karthek.android.s.files/files/magic.mgc");
    asset_g_ref = (*env)->NewGlobalRef(env, asset_manager);
    AAsset *asset = AAssetManager_open(AAssetManager_fromJava(env, asset_g_ref),
                                       "magic.mgc",
                                       O_RDONLY);
    void *buf = (void *) AAsset_getBuffer(asset);
    if (buf == NULL)
        return -1;
    void *buffers[] = {buf};
    size_t sizes[] = {AAsset_getLength(asset)};
    magic_load_buffers(magic_cookie, buffers, sizes, 1);
    return 0;
}

#ifdef NATIVE_SYNC
pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
#endif

JNIEXPORT jstring JNICALL
Java_com_karthek_android_s_files2_helpers_FileType_c_1magic_1descriptor(
        JNIEnv *env,
        __unused jobject thiz,
        jint fd) {
#ifdef NATIVE_SYNC
    pthread_mutex_lock(&mtx);
#endif
    jstring ret = (*env)->NewStringUTF(env, magic_descriptor(magic_cookie, fd));
    close(fd);
#ifdef NATIVE_SYNC
    pthread_mutex_unlock(&mtx);
#endif
    return ret;
}

JNIEXPORT void JNICALL
Java_com_karthek_android_s_files2_helpers_FileType_c_1magic_1setflags(__unused JNIEnv *env,
                                                                      __unused jobject thiz,
                                                                      jint flag) {
    if (flag == 1) {
        magic_setflags(magic_cookie, 0);
    } else {
        magic_setflags(magic_cookie, MAGIC_MIME_TYPE);
    }
}

JNIEXPORT jstring JNICALL
Java_com_karthek_android_s_files2_helpers_FileType_c_1magic_1error(JNIEnv *env,
                                                                   __unused jclass clazz) {
    return (*env)->NewStringUTF(env, magic_error(magic_cookie));
}