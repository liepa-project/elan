/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class nl_mpi_media_AudioExtractor */

#ifndef _Included_nl_mpi_media_AudioExtractor
#define _Included_nl_mpi_media_AudioExtractor
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    setDebugMode
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_nl_mpi_media_AudioExtractor_setDebugMode
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    isDebugMode
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_nl_mpi_media_AudioExtractor_isDebugMode
  (JNIEnv *, jclass);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    initLog
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_nl_mpi_media_AudioExtractor_initLog
  (JNIEnv *, jclass, jstring, jstring);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    initNative
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_nl_mpi_media_AudioExtractor_initNative
  (JNIEnv *, jobject, jstring);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSampleFrequency
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getSampleFrequency
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getBitsPerSample
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getBitsPerSample
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getNumberOfChannels
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getNumberOfChannels
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getDurationMs
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_nl_mpi_media_AudioExtractor_getDurationMs
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getDurationSec
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_nl_mpi_media_AudioExtractor_getDurationSec
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSampleBufferSize
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_nl_mpi_media_AudioExtractor_getSampleBufferSize
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSampleBufferDurationMs
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_nl_mpi_media_AudioExtractor_getSampleBufferDurationMs
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSampleBufferDurationSec
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_nl_mpi_media_AudioExtractor_getSampleBufferDurationSec
  (JNIEnv *, jobject, jlong);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSamples
 * Signature: (JDDLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getSamples
  (JNIEnv *, jobject, jlong, jdouble, jdouble, jobject);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSamplesBA
 * Signature: (JDD[B)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getSamplesBA
  (JNIEnv *, jobject, jlong, jdouble, jdouble, jbyteArray);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSample
 * Signature: (JDLjava/nio/ByteBuffer;)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getSample
  (JNIEnv *, jobject, jlong, jdouble, jobject);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    getSampleBA
 * Signature: (JD[B)I
 */
JNIEXPORT jint JNICALL Java_nl_mpi_media_AudioExtractor_getSampleBA
  (JNIEnv *, jobject, jlong, jdouble, jbyteArray);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    setPositionSec
 * Signature: (JD)Z
 */
JNIEXPORT jboolean JNICALL Java_nl_mpi_media_AudioExtractor_setPositionSec
  (JNIEnv *, jobject, jlong, jdouble);

/*
 * Class:     nl_mpi_media_AudioExtractor
 * Method:    release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_nl_mpi_media_AudioExtractor_release
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
