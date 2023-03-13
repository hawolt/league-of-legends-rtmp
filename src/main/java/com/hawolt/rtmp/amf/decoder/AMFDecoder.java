package com.hawolt.rtmp.amf.decoder;

import com.hawolt.logger.Logger;
import com.hawolt.rtmp.amf.*;
import com.hawolt.rtmp.exception.EncodingException;
import com.hawolt.rtmp.exception.NotImplementedException;
import com.hawolt.rtmp.exception.UTFException;
import com.hawolt.rtmp.utility.ByteMagic;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * AMF decoder supporting both AMF0 and AMF3
 *
 * @author Hawolt
 * @see AMF0Reader
 * @see AMF3Reader
 */

public class AMFDecoder implements BinaryReader, AMF0Reader, AMF3Reader {
    private final List<ClassDefinition> classListAMF3 = new LinkedList<>();
    private final List<Object> objectListAMF3 = new LinkedList<>();
    private final List<String> stringListAMF3 = new LinkedList<>();
    private final List<Object> objectListAMF0 = new LinkedList<>();

    protected byte[] data;
    protected int position;

    private void resetAMF0() {
        Logger.debug("AMF0 clearing references");
        objectListAMF0.clear();
    }

    private void resetBuffer() {
        this.data = new byte[0];
        this.position = 0;
    }

    @Override
    public byte read() {
        return data[position++];
    }

    @Override
    public byte[] read(int l) {
        byte[] b = new byte[l];
        for (int i = 0; i < l; i++) {
            b[i] = read();
        }
        return b;
    }

    public int readInteger() {
        int value = 0;
        for (int i = 0; i < 4; i++)
            value = (value << 8) + readByteAsInteger();

        return value;
    }

    public double readDouble() {
        long value = 0;
        for (int i = 0; i < 8; i++)
            value = (value << 8) + readByteAsInteger();

        return Double.longBitsToDouble(value);
    }

    @Override
    public Object decodeAMF0() {
        byte op = read();
        AMF0Type type = AMF0Type.find(op);
        if (type == null) throw new NotImplementedException("Unknown AMF0 type: " + op);
        Object amf0;
        switch (type) {
            case NUMBER:
                amf0 = readNumberAMF0();
                break;
            case BOOLEAN:
                amf0 = readBooleanAMF0();
                break;
            case STRING:
                amf0 = readUTF8StringAMF0();
                break;
            case OBJECT:
                amf0 = readObjectAMF0();
                break;
            case MOVIECLIP:
                amf0 = readMovieClipAMF0();
                break;
            case NULL:
                amf0 = readNullAMF0();
                break;
            case UNDEFINED:
                amf0 = readUndefinedAMF0();
                break;
            case REFERENCE:
                amf0 = readTypeReferenceAMF0();
                break;
            case MIXEDARRAY:
                amf0 = readMixedArrayAMF0();
                break;
            case OBJECTTERM:
                amf0 = readObjectTermAMF0();
                break;
            case ARRAY:
                amf0 = readArrayAMF0();
                break;
            case DATE:
                amf0 = readDateAMF0();
                break;
            case LONGSTRONG:
                amf0 = readLongStringAMF0();
                break;
            case UNSUPPORTED:
                amf0 = readUnsupportedAMF0();
                break;
            case RECORDSET:
                amf0 = readRecordSetAMF0();
                break;
            case XML:
                amf0 = readXMLAMF0();
                break;
            case TYPEDOBJECT:
                amf0 = readTypedObjectAMF0();
                break;
            case AMF3:
                amf0 = decodeAMF3();
                break;
            default:
                throw new NotImplementedException("Unknown AMF0 type: " + op);
        }
        Logger.debug("AMF0 {} as {}", type.name(), amf0 == null ? "nil" : amf0);
        return amf0;
    }


    @Override
    public String readUTF8String(int l) {
        return new String(read(l), StandardCharsets.UTF_8);
    }

    @Override
    public boolean readBoolean() {
        return read() == 1;
    }

    @Override
    public int readByteAsInteger() {
        return ((int) read()) & 0xFF;
    }

    @Override
    public double readNumberAMF0() {
        return readDouble();
    }

    @Override
    public boolean readBooleanAMF0() {
        return readBoolean();
    }

    @Override
    public String readUTF8StringAMF0() {
        int length = (readByteAsInteger() << 8) | readByteAsInteger();
        return length != 0 ? new String(read(length), StandardCharsets.UTF_8) : "";
    }

    @Override
    public TypedObject readObjectAMF0() {
        TypedObject object = new TypedObject();
        Pair<String, Object> pair;
        do {
            pair = readAMF0Pair();
            object.put(pair.getKey(), pair.getValue());
        } while (!(pair.getValue() instanceof ObjectTerminateAMF0));
        return object;
    }

    public Pair<String, Object> readAMF0Pair() {
        String key = readUTF8StringAMF0();
        Object object = decodeAMF0();
        return Pair.from(key, object);
    }

    @Override
    public Object readMovieClipAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.MOVIECLIP.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readNullAMF0() {
        return null;
    }

    @Override
    public Object readUndefinedAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readTypeReferenceAMF0() {
        int index = (readByteAsInteger() << 8) | readByteAsInteger();
        Object reference = objectListAMF0.get(index);
        Logger.debug("GET AMF0 Reference {} as {}", index, reference == null ? "nil" : reference);
        return reference;
    }

    @Override
    public Object readMixedArrayAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readObjectTermAMF0() {
        return ObjectTerminateAMF0.INSTANCE;
    }

    @Override
    public Object readArrayAMF0() {
        int entries = readInteger();
        Object[] objects = new Object[entries];
        storeObjectAMF0(objects);
        for (int i = 0; i < entries; i++) {
            objects[i] = decodeAMF0();
        }
        return objects;
    }

    @Override
    public LocalDateTime readDateAMF0() {
        long timestamp = ((Double) readNumberAMF0()).longValue();
        int length = (int) (((readByteAsInteger() << 8) | readByteAsInteger()) / 60D);
        ZoneOffset offset = ZoneOffset.ofHours(length);
        return Instant.ofEpochMilli(timestamp).atOffset(offset).toLocalDateTime();
    }

    @Override
    public String readLongStringAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readUnsupportedAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readRecordSetAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public String readXMLAMF0() {
        throw new NotImplementedException("AMF0 type " + ByteMagic.toHex(AMF0Type.UNDEFINED.getTypeIndicator()) + " is not supported");
    }

    @Override
    public TypedObject readTypedObjectAMF0() {
        TypedObject object = new TypedObject();
        storeObjectAMF0(object);
        object.put(readUTF8StringAMF0(), readObjectAMF0());
        return object;
    }

    @Override
    public void storeObjectAMF0(Object object) {
        Logger.debug("Store AMF0 Object {} at {}", object, objectListAMF0.size());
        objectListAMF0.add(object);
    }

    public TypedObject decode(byte[] b, TypedObject typedObject) throws EncodingException, NotImplementedException {
        this.resetBuffer();
        this.resetAMF0();
        this.data = b;
        if (data[0] == 0x00) {
            position++;
            typedObject.put("version", 0x00);
        }
        typedObject.put("result", decodeAMF0());
        typedObject.put("invokeId", decodeAMF0());
        typedObject.put("serviceCall", decodeAMF0());
        typedObject.put("data", decodeAMF0());
        if (position != data.length) {
            throw new EncodingException("The buffer has not been fully consumed: " + position + " of " + data.length + "\nRAW: " + ByteMagic.toHex(b));
        }
        return typedObject;
    }

    @Override
    public Object decodeAMF3() {
        byte op = read();
        AMF3Type type = AMF3Type.find(op);
        if (type == null) throw new NotImplementedException("Unknown AMF3 type: " + ByteMagic.toHex(op));
        Object amf3;
        switch (type) {
            case UNDEFINED:
                amf3 = readUndefinedAMF3();
                break;
            case NULL:
                amf3 = readNullAMF3();
                break;
            case BOOLEAN_FALSE:
                amf3 = readBooleanFalseAMF3();
                break;
            case BOOLEAN_TRUE:
                amf3 = readBooleanTrueAMF3();
                break;
            case INTEGER:
                amf3 = readIntegerAMF3();
                break;
            case DOUBLE:
                amf3 = readDoubleAMF3();
                break;
            case STRING:
                amf3 = readUTF8StringAMF3();
                break;
            case XMLDOCUMENT:
                amf3 = readXMLDocumentAMF3();
                break;
            case DATE:
                amf3 = readDateAMF3();
                break;
            case ARRAY:
                amf3 = readArrayAMF3();
                break;
            case OBJECT:
                amf3 = readObjectAMF3();
                break;
            case XML:
                amf3 = readXMLAMF3();
                break;
            case BYTEARRAY:
                amf3 = readByteArrayAMF3();
                break;
            case VECTORINT:
                amf3 = readVectorIntegerAMF3();
                break;
            case VECTORUNIT:
                amf3 = readVectorUnitAMF3();
                break;
            case VECTORDOUBLE:
                amf3 = readVectorDoubleAMF3();
                break;
            case VECTOROBJECT:
                amf3 = readVectorObjectAMF3();
                break;
            case DICTIONARY:
                amf3 = readDictionaryAMF3();
                break;
            default:
                throw new NotImplementedException("Unknown AMF3 type: " + op);
        }
        Logger.debug("AMF3 {} as {}", type.name(), amf3 == null ? "nil" : amf3);
        return amf3;
    }

    @Override
    public Object readUndefinedAMF3() {
        return "AMF3_UNDEFINED";
    }

    @Override
    public Object readNullAMF3() {
        return null;
    }

    @Override
    public boolean readBooleanFalseAMF3() {
        return false;
    }

    @Override
    public boolean readBooleanTrueAMF3() {
        return true;
    }

    @Override
    public Integer readIntegerAMF3() {
        int result = 0;
        int n = 0;
        int b = readByteAsInteger();
        while ((b & 0x80) != 0 && n < 3) {
            result <<= 7;
            result |= (b & 0x7F);
            b = readByteAsInteger();
            n++;
        }
        if (n < 3) {
            result <<= 7;
            result |= b;
        } else {
            result <<= 8;
            result |= b;
            if ((result & 0x10000000) != 0)
                result |= 0xE0000000;
        }
        return result;
    }

    @Override
    public Double readDoubleAMF3() {
        return readDouble();
    }

    @Override
    public String readUTF8StringAMF3() {
        String result;
        int type = readIntegerAMF3();
        if ((type & 0x01) == 0)
            result = getStoredStringAMF3(type >> 1);
        else {
            int length = type >> 1;
            if (length > 0) {
                byte[] bytes = read(length);
                char[] characters = new char[length];
                int c1, c2, c3, pos = 0, chars = 0;
                while (pos < length) {
                    c1 = bytes[pos++] & 0xFF;
                    if (c1 <= 0x7F) {
                        characters[chars++] = (char) c1;
                    } else {
                        switch (c1 >> 4) {
                            case 12:
                            case 13:
                                c2 = bytes[pos++];
                                if ((c2 & 0xC0) != 0x80) {
                                    throw new UTFException("Malformed input around byte " + (pos - 2));
                                }
                                characters[chars++] = (char) (((c1 & 0x1F) << 6) | (c2 & 0x3F));
                                break;
                            case 14:
                                c2 = bytes[pos++];
                                c3 = bytes[pos++];
                                if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80)) {
                                    throw new UTFException("Malformed input around byte " + (pos - 3));
                                }
                                characters[chars++] = (char) (((c1 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | ((c3 & 0x3F)));
                                break;
                            default:
                                throw new UTFException("Malformed input around byte " + (pos - 1));
                        }
                    }
                }
                result = new String(characters, 0, chars);
                storeStringAMF3(result);
            } else {
                result = "";
            }
        }
        return result;
    }

    @Override
    public Object readXMLDocumentAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.XMLDOCUMENT.getTypeIndicator()) + " is not supported");
    }

    @Override
    public LocalDateTime readDateAMF3() {
        int type = readIntegerAMF3();
        if ((type & 0x01) == 0)
            return (LocalDateTime) getStoredObjectAMF3(type >> 1);
        else {
            long timestamp = readDoubleAMF3().longValue();
            ZoneOffset offset = ZoneOffset.ofHours(0);
            LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp).atOffset(offset).toLocalDateTime();
            storeObjectAMF3(localDateTime);
            return localDateTime;
        }
    }

    @Override
    public Object[] readArrayAMF3() {
        int type = readIntegerAMF3();
        if ((type & 0x01) == 0)
            return (Object[]) getStoredObjectAMF3(type >> 1);
        else {
            final int size = type >> 1;
            String key = readUTF8StringAMF3();
            if (key.length() == 0) {
                Object[] objects = new Object[size];
                storeObjectAMF3(objects);
                for (int i = 0; i < size; i++) {
                    objects[i] = decodeAMF3();
                }
                return objects;
            } else {
                throw new NotImplementedException("Associative arrays are not supported");
            }
        }
    }

    @Override
    public Object readObjectAMF3() {
        int type = readIntegerAMF3();
        if ((type & 0x01) == 0) {
            return getStoredObjectAMF3(type >> 1);
        } else {
            boolean defineInline = (((type >> 1) & 0x01) != 0);
            ClassDefinition classDefinition;
            if (defineInline) {
                classDefinition = new ClassDefinition();
                classDefinition.externalizable = ((type >> 2 & 1) != 0);
                classDefinition.encoding = (byte) ((type >> 2) & 0x03);
                classDefinition.properties = new String[type >> 4];
                classDefinition.className = readUTF8StringAMF3();
                for (int i = 0; i < classDefinition.properties.length; i++) {
                    classDefinition.properties[i] = readUTF8StringAMF3();
                }
                storeClassDefinitionAMF3(classDefinition);
            } else {
                classDefinition = getStoredClassDefinitionAMF3(type);
            }
            TypedObject typedObject = new TypedObject(classDefinition.className);
            storeObjectAMF3(typedObject);
            if (classDefinition.externalizable) {
                switch (classDefinition.className) {
                    case "DSK":
                        typedObject = readDSK();
                        break;
                    case "DSA":
                        typedObject = readDSA();
                        break;
                    case "flex.messaging.io.ArrayCollection":
                        typedObject = TypedObject.createArrayCollection((Object[]) decodeAMF3());
                        break;
                    case "com.riotgames.platform.systemstate.ClientSystemStatesNotification":
                    case "com.riotgames.platform.broadcast.BroadcastNotification":
                    case "com.riotgames.platform.summoner.SummonerCatalog":
                    case "com.riotgames.platform.game.GameTypeConfigDTO":
                        typedObject = readJson();
                        break;
                    default:
                        throw new NotImplementedException("Unhandled Externalizable: " + classDefinition.className + "\nRAW: " + ByteMagic.toHex(data));
                }
            } else {
                for (int i = 0; i < classDefinition.properties.length; i++) {
                    typedObject.put(classDefinition.properties[i], decodeAMF3());
                }
                if (classDefinition.encoding == 0x02) {
                    while (true) {
                        String key = readUTF8StringAMF3();
                        if (key.length() == 0) break;
                        typedObject.put(key, decodeAMF3());
                    }
                }
            }
            return typedObject;
        }
    }

    @Override
    public Object readXMLAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.XML.getTypeIndicator()) + " is not supported");
    }

    @Override
    public byte[] readByteArrayAMF3() {
        int type = readIntegerAMF3();
        if ((type & 0x01) == 0) {
            return (byte[]) getStoredObjectAMF3(type >> 1);
        } else {
            byte[] bytes = read(type >> 1);
            storeObjectAMF3(bytes);
            return bytes;
        }
    }

    @Override
    public Object readVectorIntegerAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.VECTORINT.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readVectorUnitAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.VECTORUNIT.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readVectorDoubleAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.VECTORDOUBLE.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readVectorObjectAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.VECTOROBJECT.getTypeIndicator()) + " is not supported");
    }

    @Override
    public Object readDictionaryAMF3() {
        throw new NotImplementedException("AMF3 type " + ByteMagic.toHex(AMF3Type.DICTIONARY.getTypeIndicator()) + " is not supported");
    }

    @Override
    public void storeObjectAMF3(Object object) {
        Logger.debug("Store AMF3 Object {} at {}", object, objectListAMF3.size());
        objectListAMF3.add(object);
    }


    @Override
    public Object getStoredObjectAMF3(int index) {
        Object object = objectListAMF3.get(index);
        Logger.debug("GET AMF3 Object at {} as {}", index, object);
        return object;
    }

    @Override
    public void storeStringAMF3(String string) {
        Logger.debug("Store AMF3 String {} at {}", string, stringListAMF3.size());
        stringListAMF3.add(string);
    }

    @Override
    public String getStoredStringAMF3(int index) {
        String string = stringListAMF3.get(index);
        Logger.debug("GET AMF3 String at {} as {}", index, string);
        return string;
    }

    @Override
    public void storeClassDefinitionAMF3(ClassDefinition classDefinition) {
        Logger.debug("Store AMF3 ClassDefinition {} at {}", classDefinition, classListAMF3.size());
        classListAMF3.add(classDefinition);
    }

    @Override
    public ClassDefinition getStoredClassDefinitionAMF3(int index) {
        ClassDefinition classDefinition = classListAMF3.get(index);
        Logger.debug("GET AMF3 ClassDefinition at {} as {}", index, classDefinition);
        return classDefinition;
    }


    private TypedObject readJson() {
        int size = 0;
        for (int i = 0; i < 4; i++) {
            size = size * 256 + readByteAsInteger();
        }
        String json = new String(read(size));
        return TypedObject.fromJson(json);
    }

    private TypedObject readDSA() {
        TypedObject typedObject = new TypedObject("DSA");
        List<Integer> flags;
        flags = readFlagData();
        for (int i = 0; i < flags.size(); i++) {
            Integer flag = flags.get(i);
            int bits = 0;
            if (i == 0) {
                if ((flag & 0x01) != 0) typedObject.put("body", decodeAMF3());
                if ((flag & 0x02) != 0) typedObject.put("clientId", decodeAMF3());
                if ((flag & 0x04) != 0) typedObject.put("destination", decodeAMF3());
                if ((flag & 0x08) != 0) typedObject.put("headers", decodeAMF3());
                if ((flag & 0x10) != 0) typedObject.put("messageId", decodeAMF3());
                if ((flag & 0x20) != 0) typedObject.put("timeStamp", decodeAMF3());
                if ((flag & 0x40) != 0) typedObject.put("timeToLive", decodeAMF3());
                bits = 7;
            } else if (i == 1) {
                if ((flag & 0x01) != 0) {
                    typedObject.put("clientId", convertByteArrayToId((byte[]) decodeAMF3()));
                }
                if ((flag & 0x02) != 0) {
                    typedObject.put("messageId", convertByteArrayToId((byte[]) decodeAMF3()));
                }
                bits = 2;
            }
            readRemaining(flag, bits);
        }
        flags = readFlagData();
        for (int i = 0; i < flags.size(); i++) {
            Integer flag = flags.get(i);
            int bits = 0;
            if (i == 0) {
                if ((flag & 0x01) != 0) typedObject.put("correlationId", decodeAMF3());
                if ((flag & 0x02) != 0) {
                    byte ignored = read();
                    Logger.info("Ignoring byte {}", ByteMagic.toHex(ignored));
                    typedObject.put("correlationId", convertByteArrayToId(readByteArrayAMF3()));
                }
                bits = 2;
            }
            readRemaining(flag, bits);
        }
        return typedObject;
    }

    private Object convertByteArrayToId(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) builder.append('-');
            builder.append(String.format("%02x", bytes[i]));
        }
        return builder.toString();
    }

    private List<Integer> readFlagData() {
        List<Integer> flags = new ArrayList<>();
        int flag;
        do {
            flags.add(flag = readByteAsInteger());
        } while ((flag & 0x80) != 0);
        return flags;
    }

    private void readRemaining(int flag, int bits) {
        if ((flag >> bits) != 0) {
            for (int i = bits; i < 6; i++) {
                if (((flag >> i) & 1) != 0) {
                    Object o = decodeAMF3();
                    Logger.info("Ignoring AMF3 {}", o);
                }
            }
        }
    }

    private TypedObject readDSK() {
        TypedObject typedObject = readDSA();
        List<Integer> flags = readFlagData();
        for (Integer flag : flags) {
            readRemaining(flag, 0);
        }
        return typedObject;
    }
}
