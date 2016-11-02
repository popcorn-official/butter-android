/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.utils;

import org.mozilla.universalchardet.Constants;
import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;

import butter.droid.base.content.UnicodeBOMInputStream;

public class FileUtils {

    private static HashMap<String, String> sOverrideMap;

    static {
        sOverrideMap = new HashMap<>();
        sOverrideMap.put("tr", "ISO-8859-9");
        sOverrideMap.put("sr", "Windows-1250");
        sOverrideMap.put("ro", "ISO-8859-2");
    }

    /**
     * Get contents of a file as String
     *
     * @param filePath File path as String
     * @return Contents of the file
     * @throws IOException
     */
    public static String getContentsAsString(String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    /**
     * Convert an {@link InputStream} to a String
     *
     * @param inputStream InputStream
     * @return String contents of the InputStream
     * @throws IOException
     */
    private static String convertStreamToString(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    /**
     * Delete every item below the File location
     *
     * @param file Location
     */
    public static boolean recursiveDelete(File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            if (children == null) return false;
            for (String child : children) {
                recursiveDelete(new File(file, child));
            }
        }

        return file.delete();
    }

    /**
     * Get the charset of the contents of an {@link InputStream}
     *
     * @param inputStream {@link InputStream}
     * @return Charset String name
     * @throws IOException
     */
    public static String inputstreamToCharsetString(InputStream inputStream) throws IOException {
        return inputstreamToCharsetString(inputStream, null);
    }

    /**
     * Get the charset of the contents of an {@link InputStream}
     *
     * @param inputStream {@link InputStream}
     * @param languageCode Language code for charset override
     * @return Charset String name
     * @throws IOException
     */
    public static String inputstreamToCharsetString(InputStream inputStream, String languageCode) throws IOException {
        UniversalDetector charsetDetector = new UniversalDetector(null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        UnicodeBOMInputStream bomInputStream = new UnicodeBOMInputStream(inputStream);
        bomInputStream.skipBOM();
        byte data[] = new byte[1024];
        int count;
        while ((count = bomInputStream.read(data)) != -1) {
            if (!charsetDetector.isDone()) {
                charsetDetector.handleData(data, 0, count);
            }
            byteArrayOutputStream.write(data, 0, count);
        }
        charsetDetector.dataEnd();

        String detectedCharset = charsetDetector.getDetectedCharset();
        charsetDetector.reset();

        if (detectedCharset == null || detectedCharset.isEmpty()) {
            // UniversalDetector can't detect the charset so try to get charset from BOM.
            detectedCharset = getCharsetFromBOM(bomInputStream.getBOM());
        } else if ("MACCYRILLIC".equals(detectedCharset)) {
            detectedCharset = "Windows-1256";
        }

        if (languageCode != null && sOverrideMap.containsKey(languageCode) && !detectedCharset.equals("UTF-8")) {
            detectedCharset = sOverrideMap.get(languageCode);
        }

        byte[] stringBytes = byteArrayOutputStream.toByteArray();
        Charset charset = Charset.forName(detectedCharset);
        CharsetDecoder decoder = charset.newDecoder();

        try {
            CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(stringBytes));
            return charBuffer.toString();
        } catch (CharacterCodingException e) {
            return new String(stringBytes, detectedCharset);
        }
    }

    /**
     * Save {@link InputStream} to {@link File}
     *
     * @param inputStream InputStream that will be saved
     * @param path        Path of the file
     * @throws IOException
     */
    public static void saveStringFile(InputStream inputStream, File path) throws IOException {
        String outputString = inputstreamToCharsetString(inputStream, null);
        saveStringToFile(outputString, path, "UTF-8");
    }

    /**
     * Save {@link String} to {@link File}
     *
     * @param inputStr String that will be saved
     * @param path     Path of the file
     * @throws IOException
     */
    public static void saveStringFile(String inputStr, File path) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputStr.getBytes());
        saveStringFile(inputStream, path);
    }

    /**
     * Save {@link String} array  to {@link File}
     *
     * @param inputStr String array that will be saved
     * @param path     {@link File}
     * @throws IOException
     */
    public static void saveStringFile(String[] inputStr, File path) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : inputStr) {
            stringBuilder.append(str).append("\n");
        }
        saveStringFile(stringBuilder.toString(), path);
    }

    /**
     * Save {@link String} to {@link File} witht the specified encoding
     *
     * @param string {@link String}
     * @param path   Path of the file
     * @param string Encoding
     * @throws IOException
     */
    public static void saveStringToFile(String string, File path, String encoding) throws IOException {
        if (path.exists()) {
            path.delete();
        }

        if ((path.getParentFile().mkdirs() || path.getParentFile().exists()) && (path.exists() || path.createNewFile())) {
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), encoding));
            writer.write(string);
            writer.close();
        }
    }

    /**
     * Get the extension of the file
     *
     * @param fileName Name (and location) of the file
     * @return Extension
     */
    public static String getFileExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

        if (i > p) {
            extension = fileName.substring(i + 1);
        }

        return extension;
    }

    /**
     * Copy file (only use for files smaller than 2GB)
     *
     * @param src Source
     * @param dst Destionation
     * @throws IOException
     */
    public static void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        try {
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            inStream.close();
            outStream.close();
        }
    }


    /**
     * Get the charset from BOM if available.
     *
     * @param bom BOM of the file
     * @return Charset
     */
    public static String getCharsetFromBOM(UnicodeBOMInputStream.BOM bom) {
        if (UnicodeBOMInputStream.BOM.UTF_32_BE.equals(bom)) {
            return Constants.CHARSET_UTF_32BE;
        } else if (UnicodeBOMInputStream.BOM.UTF_32_LE.equals(bom)) {
            return Constants.CHARSET_UTF_32LE;
        } else if (UnicodeBOMInputStream.BOM.UTF_8.equals(bom)) {
            return Constants.CHARSET_UTF_8;
        } else if (UnicodeBOMInputStream.BOM.UTF_16_BE.equals(bom)) {
            return Constants.CHARSET_UTF_16BE;
        } else if (UnicodeBOMInputStream.BOM.UTF_16_LE.equals(bom)) {
            return Constants.CHARSET_UTF_16LE;
        }

        return "UTF-8";
    }
}
