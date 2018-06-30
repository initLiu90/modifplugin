package com.lzp.modifyplugin;

class Constants {
    static int RESCHUNK_HEADER_SIZE = 8

    public static int RES_TABLE_TYPE_TYPE = 0x0201//ResTable_type
    public static int RES_TABLE_TYPE_SPEC_TYPE = 0x0202//ResTable_typeSpec
    public static int RES_TABLE_LIBRARY_TYPE = 0x0203//ResTable_lib_header

    public static int ResTable_entry_FLAG_COMPLEX = 0x0001

    public static int LIBRARY_HEADER_SIZE = 0x0C
    public static int LIBRARY_CHUNK_SIZE = 272//ResTable_lib_header & ResTable_lib_entry
}
