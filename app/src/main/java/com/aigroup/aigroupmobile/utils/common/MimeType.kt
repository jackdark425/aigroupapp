package com.aigroup.aigroupmobile.utils.common

/**
 * See [Media Types](https://www.iana.org/assignments/media-types/media-types.xhtml) of IANA.
 */
object MimeTypes {

  interface BaseMimeType {
    val mimeType: String
    val extensions: Array<String>

    fun equalsMime(mimeType: String): Boolean {
      return this.mimeType.lowercase() == mimeType.lowercase()
    }
  }

  enum class Application(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** AbiWord document */
    ABI_WORD(arrayOf(".abw"), "application/x-abiword"),

    /** Archive document (multiple files embedded) */
    ARCHIVE(arrayOf(".arc"), "application/x-freearc"),

    /** Atom Documents */
    ATOM_XML(arrayOf(".atom"), "application/atom+xml"),

    /** Atom Category Documents */
    ATOMCAT_XML(arrayOf(".atomcat"), "application/atomcat+xml"),

    /** Amazon Kindle eBook format */
    AMAZON_KINDLE(arrayOf(".azw"), "application/vnd.amazon.ebook"),

    /** Apple Installer Package */
    APPLE_INSTALLER(arrayOf(".mpkg"), "application/vnd.apple.installer+xml"),

    /** C-Shell script */
    CSH(arrayOf(".csh"), "application/x-csh"),

    /** ECMAScript */
    ECMASCRIPT(arrayOf(".es"), "application/ecmascript"),

    /** Electronic publication (EPUB) */
    EPUB(arrayOf(".epub"), "application/epub+zip"),

    /** GZip Compressed Archive */
    GZIP(arrayOf(".gz"), "application/gzip"),

    /** Java Archive (JAR) */
    JAVA_ARCHIVE(arrayOf(".jar"), "application/java-archive"),

    /** JavaScript */
    JAVASCRIPT(arrayOf(".js"), "application/javascript"),

    /** JSON format */
    JSON(arrayOf(".json"), "application/json"),

    /** JSON-LD format */
    JSON_LD(arrayOf(".jsonld"), "application/ld+json"),

    /** Microsoft Excel */
    MS_EXCEL(arrayOf(".xls"), "application/vnd.ms-excel"),

    /** Microsoft Excel (OpenXML) */
    MS_EXCEL_OPEN_XML(arrayOf(".xlsx"), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

    /** MS Embedded OpenType fonts */
    MS_OPENTYPE_FONTS(arrayOf(".eot"), "application/vnd.ms-fontobject"),

    /** Microsoft PowerPoint */
    MS_POWER_POINT(arrayOf(".ppt"), "application/vnd.ms-powerpoint"),

    /** Microsoft PowerPoint (OpenXML) */
    MS_POWER_POINT_OPEN_XML(arrayOf(".pptx"), "application/vnd.openxmlformats-officedocument.presentationml.presentation"),

    /** Microsoft Word */
    MS_WORD(arrayOf(".doc"), "application/msword"),

    /** Microsoft Word (OpenXML) */
    MS_WORD_OPEN_XML(arrayOf(".docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

    /** Microsoft Visio */
    MS_VISIO(arrayOf(".vsd"), "application/vnd.visio"),

    /** Any kind of binary data */
    OCTET_STREAM(arrayOf(".bin"), "application/octet-stream"),

    /** OGG */
    OGG(arrayOf(".ogx"), "application/ogg"),

    /** OpenDocument presentation document */
    OPEN_DOCUMENT_PRESENTATION(arrayOf(".odp"), "application/vnd.oasis.opendocument.presentation"),

    /** OpenDocument spreadsheet document */
    OPEN_DOCUMENT_SPREADSHEET(arrayOf(".ods"), "application/vnd.oasis.opendocument.spreadsheet"),

    /** OpenDocument text document */
    OPEN_DOCUMENT_TEXT(arrayOf(".odt"), "application/vnd.oasis.opendocument.text"),

    /** Adobe Portable Document Format (PDF) */
    PDF(arrayOf(".pdf"), "application/pdf"),

    /** Hypertext Preprocessor (Personal Home Page) */
    PHP(arrayOf(".php"), "application/x-httpd-php"),

    /** PKCS #10 */
    PKCS_10(arrayOf(".p10"), "application/pkcs10"),

    /** PKCS #7 MIME */
    PKCS_7_MIME(arrayOf(".p7m"), "application/pkcs7-mime"),

    /** PKCS #7 Signature */
    PKCS_7_SIGNATURE(arrayOf(".p7s"), "application/pkcs7-signature"),

    /** PKCS #8 */
    PKCS_8(arrayOf(".p8"), "application/pkcs8"),

    /** PKCS #12 */
    PKCS_12(arrayOf(".p12", ".pfx"), "application/x-pkcs12"),

    /** PostScript */
    POSTSCRIPT(arrayOf(".ps"), "application/postscript"),

    /** RAR archive */
    RAR(arrayOf(".rar"), "application/vnd.rar"),

    /** RDF/XML */
    RDF_XML(arrayOf(".rdf"), "application/rdf+xml"),

    /** Rich Text Format (RTF) */
    RTF(arrayOf(".rtf"), "application/rtf"),

    /** 7-zip archive */
    SEVEN_ZIP(arrayOf(".7z"), "application/x-7z-compressed"),

    /** Bourne shell script */
    SH(arrayOf(".sh"), "application/x-sh"),

    /** SMIL documents */
    SMIL_XML(arrayOf(".smil", ".smi", ".sml"), "application/smil+xml"),

    /** SQL */
    SQL(arrayOf(".sql"), "application/sql"),

    /** Small web format (SWF) or Adobe Flash document */
    SWF(arrayOf(".swf"), "application/x-shockwave-flash"),

    /** Tape Archive (TAR) */
    TAR(arrayOf(".tar"), "application/x-tar"),

    /** BZip Archive */
    X_BZIP(arrayOf(".bz"), "application/x-bzip"),

    /** BZip2 Archive */
    X_BZIP2(arrayOf(".bz2"), "application/x-bzip2"),

    /** XHTML */
    XHTML_XML(arrayOf(".xhtml"), "application/xhtml+xml"),

    /** XML */
    XML(arrayOf(".xml"), "application/xml"),

    /** XML DTD */
    XML_DTD(arrayOf(".dtd", ".mod"), "application/xml-dtd"),

    /** XSLT Document */
    XSLT_XML(arrayOf(".xsl", ".xslt"), "application/xslt+xml"),

    /** XUL */
    XUL(arrayOf(".xul"), "application/vnd.mozilla.xul+xml"),

    /** Zip Archive */
    ZIP(arrayOf(".zip"), "application/zip"),

    /** freemind mindmapping file */
    FREEMIND(arrayOf(".mm"), "application/x-freemind"),
  }

  enum class Audio(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** AAC audio */
    AAC(arrayOf(".aac"), "audio/aac"),

    /** Musical Instrument Digital Interface (MIDI) */
    MIDI(arrayOf(".mid", ".midi"), "audio/midi"),

    /** Musical Instrument Digital Interface (MIDI) */
    X_MIDI(arrayOf(".mid", ".midi"), "audio/x-midi"),

    /** Matroska Multimedia Container */
    MKA(arrayOf(".mka"), "audio/x-matroska"),

    /** MP3 audio */
    MP3(arrayOf(".mp3"), "audio/mpeg"),

    /** MP4 audio */
    MP4(arrayOf(".mp4"), "audio/mp4"),

    /** OGG audio */
    OGG(arrayOf(".oga"), "audio/ogg"),

    /** Opus audio */
    OPUS(arrayOf(".opus"), " audio/opus"),

    /** Waveform Audio Format */
    WAV(arrayOf(".wav"), "audio/wav"),

    /** WEBM audio */
    WEBM(arrayOf(".weba"), "audio/webm")
  }

  enum class Image(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** Windows OS/2 Bitmap Graphics */
    BMP(arrayOf(".bmp"), "image/bmp"),

    /** Graphics Interchange Format (GIF)*/
    GIF(arrayOf(".gif"), "image/gif"),

    /** Icon format */
    ICON(arrayOf(".ico"), "image/vnd.microsoft.icon"),

    /** JPEG images */
    JPEG(arrayOf(".jpg", ".jpeg"), "image/jpeg"),

    /** Portable Network Graphics */
    PNG(arrayOf(".png"), "image/png"),

    /** Scalable Vector Graphics (SVG) */
    SVG_XML(arrayOf(".svg"), "image/svg+xml"),

    /** Tagged Image File Format (TIFF) */
    TIFF(arrayOf(".tif", ".tiff"), "image/tiff"),

    /** WEBP image */
    WEBP(arrayOf(".webp"), "image/webp"),

    /** HEIC image */
    HEIC(arrayOf(".heic"), "image/heic"),

    /** HEIF image */
    HEIF(arrayOf(".heif"), "image/heif"),

    /** AVIF image */
    AVIF(arrayOf(".avif", ".avifs"), "image/avif"),
  }

  enum class Text(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** Cascading Style Sheets (CSS) */
    CSS(arrayOf(".css"), "text/css"),

    /** Comma-separated values (CSV) */
    CSV(arrayOf(".csv"), "text/csv"),

    /** HyperText Markup Language (HTML) */
    HTML(arrayOf(".htm", ".html"), "text/html"),

    /** iCalendar format */
    ICALENDAR(arrayOf(".ics"), "text/calendar"),

    /** JavaScript */
    JAVASCRIPT_MODULE(arrayOf(".mjs"), "text/javascript"),

    /** Text, (generally ASCII or ISO 8859-n) */
    PLAIN(arrayOf(".txt"), "text/plain"),

    /** Standard Generalized Markup Language */
    SGML(arrayOf(".sgml"), "text/sgml"),

    /** YAML */
    YAML(arrayOf(".yml", ".yaml"), "text/yaml"),
  }

  enum class Video(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** AVI: Audio Video Interleave */
    AVI(arrayOf(".avi"), "video/x-msvideo"),

    /** 3GPP audio/video container */
    THREEGPP(arrayOf(".3gp"), "video/3gpp"),

    /** 3GPP2 audio/video container */
    THREEGPP2(arrayOf(".3g2"), "video/3gpp2"),

    /** Matroska Multimedia Container */
    MKV(arrayOf(".mkv"), "video/x-matroska"),

    /** MP4 video */
    MP4(arrayOf(".mp4"), "video/mp4"),

    /** MPEG Video */
    MPEG(arrayOf(".mpg", ".mpeg"), "video/mpeg"),

    /** MPEG transport stream */
    MPEG_TS(arrayOf(".ts"), "video/mp2t"),

    /** OGG video */
    OGG(arrayOf(".ogv"), "video/ogg"),

    /** QuickTime */
    QUICKTIME(arrayOf(".mov", ".qt"), "video/quicktime"),

    /** WEBM video */
    WEBM(arrayOf(".webm"), "video/webm")
  }

  enum class Font(override val extensions: Array<String>, override val mimeType: String): BaseMimeType {
    /** OpenType font */
    OTF(arrayOf(".otf"), "font/otf"),

    /** TrueType Font */
    TTF(arrayOf(".ttf"), "font/ttf"),

    /** Web Open Font Format (WOFF) */
    WOFF(arrayOf(".woff"), "font/woff"),

    /** Web Open Font Format (WOFF) */
    WOFF2(arrayOf(".woff2"), "font/woff2")
  }
}