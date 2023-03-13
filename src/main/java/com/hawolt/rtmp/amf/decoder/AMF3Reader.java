package com.hawolt.rtmp.amf.decoder;

import com.hawolt.rtmp.amf.ClassDefinition;

import java.time.LocalDateTime;

/**
 * Decoder capabilities for AMF3
 *
 * @author Hawolt
 */

public interface AMF3Reader {

    Object decodeAMF3();

    Object readUndefinedAMF3();

    Object readNullAMF3();

    boolean readBooleanFalseAMF3();

    boolean readBooleanTrueAMF3();

    Integer readIntegerAMF3();

    Double readDoubleAMF3();

    String readUTF8StringAMF3();

    Object readXMLDocumentAMF3();

    LocalDateTime readDateAMF3();

    Object[] readArrayAMF3();

    Object readObjectAMF3();

    Object readXMLAMF3();

    byte[] readByteArrayAMF3();

    Object readVectorIntegerAMF3();

    Object readVectorUnitAMF3();

    Object readVectorDoubleAMF3();

    Object readVectorObjectAMF3();

    Object readDictionaryAMF3();

    void storeObjectAMF3(Object object);

    Object getStoredObjectAMF3(int index);

    void storeStringAMF3(String string);

    String getStoredStringAMF3(int index);

    void storeClassDefinitionAMF3(ClassDefinition classDefinition);

    ClassDefinition getStoredClassDefinitionAMF3(int index);
}
