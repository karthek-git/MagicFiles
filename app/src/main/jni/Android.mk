LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := magic-wrapper
LOCAL_SRC_FILES := magic-wrapper.c
LOCAL_SHARED_LIBRARIES := magic
LOCAL_LDLIBS := -landroid
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := magic
LOCAL_SRC_FILES := libmagic.so
include $(PREBUILT_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := archive-wrapper
#LOCAL_SRC_FILES := archive-wrapper.c
#LOCAL_SHARED_LIBRARIES := archive lzma
#LOCAL_LDLIBS := -lz
#include $(BUILD_SHARED_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := archive
#LOCAL_SRC_FILES := libarchive.a
#include $(PREBUILT_STATIC_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := lzma
#LOCAL_SRC_FILES := liblzma.a
#include $(PREBUILT_STATIC_LIBRARY)
#
#include $(CLEAR_VARS)
#LOCAL_MODULE := xml2
#LOCAL_SRC_FILES := libxml2.a
#include $(PREBUILT_STATIC_LIBRARY)