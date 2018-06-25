package com.lzp.modifyplugin;

/**
 * Created by lillian on 2018/6/24.
 */

public class XmlUtils {
    private static final short RES_XML_START_NAMESPACE_TYPE = 0x0100;
    private static final short RES_XML_START_ELEMENT_TYPE = 0x0102
    private static final short RES_XML_END_ELEMENT_TYPE = 0x0103
    private static final short RES_XML_END_NAMESPACE_TYPE = 0x0101

    static void changePackageId(byte[] src, int packageId) {
        //step1:跳过ResXMLTree_header(ResXMLTree_header--8)
        int offset = 8;

        //step2:读取ResStringPool_header（资源字符串） chunk的大小
        //通过ResStringPool_header.ResChunk_header.size获取
        byte[] tableSizeBytes = Utils.copyByte(src, offset + 2 + 2, 4)
        offset += Utils.byte2int(tableSizeBytes)

        //step3:读取ResChunk_header(系统资源id)
        byte[] systemIdsBytes = Utils.copyByte(src, offset + 2 + 2, 4)
        offset += Utils.byte2int(systemIdsBytes)

        //step4:RES_XML_START_NAMESPACE_TYPE
        byte[] namespaceChunkBytes = Utils.copyByte(src, offset + 2 + 2, 4)
        offset += Utils.byte2int(namespaceChunkBytes)

        parseXmlElement(src, offset, packageId)
    }

    private static void parseXmlElement(byte[] src, int offset, int packageId) {
        while (offset < src.length) {
            //step5:RES_XML_START_ELEMENT_TYPE
            byte[] typeBytes = Utils.copyByte(src, offset, 2)
            short type = Utils.byte2Short(typeBytes)

            switch (type) {
                case RES_XML_START_ELEMENT_TYPE:
                    offset += 16

                    //step6:ResXMLTree_attrExt
                    short attributeStart = Utils.byte2Short(Utils.copyByte(src, offset + 4 + 4, 2))
                    short attributeCount = Utils.byte2Short(Utils.copyByte(src, offset + 4 + 4 + 2 * 2, 2))
                    offset += attributeStart

                    for (int i = 0; i < attributeCount; i++) {
                        byte[] dataBytes = Utils.copyByte(src, offset + 4 + 4 + 4 + 2 + 1 + 1, 4)
                        int data = Utils.byte2int(dataBytes)
                        String resId = Integer.toHexString(data);
                        if (resId.startsWith('7f')) {
//                            println("oldResId=" + resId)
//                            dataBytes.each {
//                                print(it + ' ')
//                            }
//                            println()

                            String newPackageId = Integer.toHexString(packageId)
                            String newResId = resId.replace('7f', newPackageId)
//                            println("newResId=" + newResId)
                            byte[] newResIdBytes = Utils.int2ByteArray(Integer.decode('0x' + newResId))
                            modifyXmlContent(src, newResIdBytes, offset + 4 + 4 + 4 + 2 + 1 + 1, 4)
                        }
                        offset += 20
                    }
                    break
                case RES_XML_END_ELEMENT_TYPE:
                    offset += 16
                    //跳过ResXMLTree_endElementExt
                    offset += 8
                    break
                case RES_XML_START_NAMESPACE_TYPE:
                    byte[] namespaceChunkBytes = Utils.copyByte(src, offset + 2 + 2, 4)
                    offset += Utils.byte2int(namespaceChunkBytes)
                    break
                case RES_XML_END_NAMESPACE_TYPE:
                    offset += 16
                    return
                default:
                    return
            }
        }
    }

    private static modifyXmlContent(byte[] content, byte[] dest, int start, int len) {
        for (int i = 0; i < len; i++) {
            content[start + i] = dest[i]
        }
    }

    static generateNewXmlFile(java.lang.String path, java.lang.String fileName, byte[] src) {
//        println("### path=" + path + ",name=" + fileName)
        def file = new File(path, fileName)
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs()
        }

        file.withOutputStream {
            it.write(src)
            it.flush()
        }
        return file
    }
}
