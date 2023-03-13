package com.hawolt.rtmp.amf.decoder;

import com.hawolt.rtmp.amf.TypedObject;

import java.time.LocalDateTime;

/**
 * Decoder capabilities for AMF0
 *
 * @author Hawolt
 */

public interface AMF0Reader {
    Object decodeAMF0();

    double readNumberAMF0();

    boolean readBooleanAMF0();

    String readUTF8StringAMF0();

    Object readObjectAMF0();

    Object readMovieClipAMF0();

    Object readNullAMF0();

    Object readUndefinedAMF0();

    Object readTypeReferenceAMF0();

    Object readMixedArrayAMF0();

    Object readObjectTermAMF0();

    Object readArrayAMF0();

    LocalDateTime readDateAMF0();

    String readLongStringAMF0();

    Object readUnsupportedAMF0();

    Object readRecordSetAMF0();

    String readXMLAMF0();

    TypedObject readTypedObjectAMF0();

    void storeObjectAMF0(Object object);
}
