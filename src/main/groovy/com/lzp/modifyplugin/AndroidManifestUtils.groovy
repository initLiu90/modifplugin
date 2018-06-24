package com.lzp.modifyplugin;

/**
 * Created by lillian on 2018/6/24.
 */

public class AndroidManifestUtils {
    private static final short RES_XML_START_ELEMENT_TYPE = 0x0102
    private static final short RES_XML_END_ELEMENT_TYPE = 0x0103
    private static final short RES_XML_END_NAMESPACE_TYPE = 0x0101

    static void changePackageId(byte[] src, int packageId) {
        println("####### changePackageId")
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

        parseXmlElement(src, offset)
    }

    private static void parseXmlElement(byte[] src, int offset) {
        while (offset < src.length) {
            //step5:RES_XML_START_ELEMENT_TYPE
            byte[] typeBytes = Utils.copyByte(src, offset, 2)
            short type = Utils.byte2Short(typeBytes)
            offset += 16

            switch (type) {
                case RES_XML_START_ELEMENT_TYPE:
                    //step6:ResXMLTree_attrExt
                    short attributeStart = Utils.byte2Short(Utils.copyByte(src, offset + 4 + 4, 2))
                    short attributeCount = Utils.byte2Short(Utils.copyByte(src, offset + 4 + 4 + 2 * 2, 2))
                    println("####### attributeCount=" + attributeCount)
                    offset += attributeStart

                    for (int i = 0; i < attributeCount; i++) {
                        int data = Utils.byte2int(Utils.copyByte(src, offset + 4 + 4 + 4 + 2 + 1 + 1, 4))
                        println("########## resid=0x" + Integer.toHexString(data))
                        offset += 20
                    }
                    break
                case RES_XML_END_ELEMENT_TYPE:
                    offset += 8
                    break
                case RES_XML_END_NAMESPACE_TYPE:
                    return
                default:
                    return
            }
        }
    }
}
