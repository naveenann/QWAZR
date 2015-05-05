/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray */

#ifndef _Included_com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
#define _Included_com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
 * Method:    init
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_opensearchserver_utils_array_NativeFloatBufferedArray_init
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
 * Method:    free
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_opensearchserver_utils_array_NativeFloatBufferedArray_free
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
 * Method:    add
 * Signature: (J[FI)V
 */
JNIEXPORT void JNICALL Java_com_opensearchserver_utils_array_NativeFloatBufferedArray_add
  (JNIEnv *, jobject, jlong, jfloatArray, jint);

/*
 * Class:     com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
 * Method:    getSize
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_opensearchserver_utils_array_NativeFloatBufferedArray_getSize
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_jaeksoft_searchlib_util_array_NativeFloatBufferedArray
 * Method:    populateFinalArray
 * Signature: (J[F)V
 */
JNIEXPORT void JNICALL Java_com_opensearchserver_utils_array_NativeFloatBufferedArray_populateFinalArray
  (JNIEnv *, jobject, jlong, jfloatArray);

#ifdef __cplusplus
}
#endif
#endif
