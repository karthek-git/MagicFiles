#include <jni.h>
#include <syslog.h>
#include <include/archive.h>
#include <include/archive_entry.h>


struct archive *a;
struct archive_entry *entry;
int r;

JNIEXPORT jlong JNICALL
Java_com_karthek_android_s_files_helper_FArchive_c_1archive_1list(__unused JNIEnv *env,
                                                                  __unused jclass clazz,
                                                                  jint fd) {
    a = archive_read_new();
    archive_read_support_filter_all(a);
    archive_read_support_format_all(a);
    r = archive_read_open_fd(a, fd, 10240);
    if (r != ARCHIVE_OK) {
        syslog(LOG_INFO, "%s\n", archive_error_string(a));
        return (1);
    }
    while (archive_read_next_header(a, &entry) == ARCHIVE_OK) {
        syslog(LOG_INFO, "f:%s\n", archive_entry_pathname(entry));
        archive_read_data_skip(a);
    }
    r = archive_read_free(a);
    if (r != ARCHIVE_OK)
        return (1);
    return 0;
}


JNIEXPORT jint JNICALL
Java_com_karthek_android_s_files_helper_FArchive_c_1archive_1extract(JNIEnv *env,
                                                                     __unused jclass clazz,
                                                                     jint fd,
                                                                     jstring target) {
    a = archive_read_new();
    archive_read_support_filter_all(a);
    archive_read_support_format_all(a);
    r = archive_read_open_fd(a, fd, 10240);
    if (r != ARCHIVE_OK) {
        syslog(LOG_INFO, "err: %s\n", archive_error_string(a));
        return (1);
    }
    if (chdir((*env)->GetStringUTFChars(env, target, NULL)) != 0)
        return -1;
    int flags = ARCHIVE_EXTRACT_TIME;
    flags |= ARCHIVE_EXTRACT_PERM;
    flags |= ARCHIVE_EXTRACT_ACL;
    flags |= ARCHIVE_EXTRACT_FFLAGS;

    for (;;) {
        r = archive_read_next_header(a, &entry);
        if (r == ARCHIVE_EOF)
            break;
        archive_read_extract(a, entry, flags);
        if (r != ARCHIVE_OK) {
            syslog(LOG_INFO, "err: %s\n", archive_error_string(a));
            return r;
        }
    }

    r = archive_read_free(a);
    if (r != ARCHIVE_OK)
        return (1);
    return 0;
}
