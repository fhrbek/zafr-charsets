package cz.zafr.charsets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Map;
import java.util.TreeMap;

public class Generator {
	
	private static final int CHAR_FROM = 0x81; 
	
	private static final int CHAR_TO = 0xff; 

	private static final ByteBuffer HIGH_ASCII_BYTES = generateBytes(CHAR_FROM, CHAR_TO);
	
	private static class Mapping {
		Charset charset;
		byte byteChar;
		
		Mapping(Charset charset, byte byteChar) {
			this.charset = charset;
			this.byteChar = byteChar;
		}
		
		byte charsetAsByte() {
			String name = charset.displayName();
			return Byte.valueOf(name.substring(name.length()-1));
		}
	}

	public static void main(String... args) throws CharacterCodingException {
		if (args.length > 0) {
			Charset[] charsets = getCharsets(args);
			TreeMap<Long, Mapping> conversionTable = new TreeMap<Long, Mapping>();
			
			for (Charset charset : charsets) {
				addMapping(conversionTable, charset);
			}
			
			printTestReport(conversionTable);
			printData(conversionTable);
		} else {
			printUsage();
		}
	}

	private static void printUsage() {
		System.out.println("Usage: java -jar i18n-char-map-generator.jar <charset names as cpXXXX>");
	}

	private static void printData(TreeMap<Long, Mapping> conversionTable) {
		System.out.println("--- DATA (" + conversionTable.size() + " items) ---");
		for (Map.Entry<Long, Mapping> entry : conversionTable.entrySet()) {
			System.out.println(String.format("{0x%04X, 0x%02X, 0x%02x}", entry.getKey(), Byte.toUnsignedInt(entry.getValue().charsetAsByte()), Byte.toUnsignedInt(entry.getValue().byteChar)));
		}
	}

	private static void printTestReport(TreeMap<Long, Mapping> conversionTable) {
		System.out.println("--- TEST REPORT (" + conversionTable.size() + " items) ---");
		for (Map.Entry<Long, Mapping> entry : conversionTable.entrySet()) {
			long uchar = entry.getKey();
			int charset = Byte.toUnsignedInt(entry.getValue().charsetAsByte());
			byte targetByte = entry.getValue().byteChar;
			String charsetName = "windows-125" + charset;
			CharBuffer ucharDecoded = Charset.forName("unicode").decode(ByteBuffer.wrap(new byte[] {(byte) ((uchar & 0xff00) >> 8), (byte) (uchar & 0x00ff)}));
			CharBuffer charFromSet = Charset.forName(charsetName).decode(ByteBuffer.wrap(new byte[] {targetByte}));
			System.out.println(
					"0x" + String.format("%04X", uchar) + " (" + ucharDecoded.charAt(0) + ") -> " +
					charsetName + "/0x" + Integer.toHexString(Byte.toUnsignedInt(targetByte)) + " (" + charFromSet.charAt(0) + ")");
		}
	}

	private static ByteBuffer generateBytes(int from, int to) {
		byte[] bytes = new byte[to - from + 1];
		for (int i=0; i < bytes.length; i++) {
			bytes[i] = (byte) (i + from);
		}
		
		return ByteBuffer.wrap(bytes);
	}

	private static void addMapping(
			TreeMap<Long, Mapping> conversionTable,
			Charset charset) throws CharacterCodingException {

		System.out.println("Processing " + charset + "...");
		CharBuffer chb = charset.newDecoder()
			        .onMalformedInput(CodingErrorAction.REPLACE)
			        .onUnmappableCharacter(CodingErrorAction.REPLACE)
			        .replaceWith("\u0000").decode((ByteBuffer) HIGH_ASCII_BYTES.rewind());

		for (int i=0; i < chb.length(); i++) {
			int asciiCode = i + CHAR_FROM;
			char c = chb.charAt(i);
			if (c != 0) {
				conversionTable.put((long) c, new Mapping(charset, (byte) asciiCode));
			} else {
				System.out.println("  Warning: char 0x" + String.format("%02X", asciiCode) + " has no unicode mapping and will be skipped");
			}
		}
	}

	private static Charset[] getCharsets(String[] args) {
		Charset[] charsets = new Charset[args.length];
		int index = 0;
		
		for (String charsetKey : args) {
			if (charsetKey.matches("cp\\d{4}")) {
				charsets[index++] = Charset.forName(charsetKey);
			} else {
				throw new IllegalArgumentException("Code page " + charsetKey + " does not match pattern 'cpXXXX'");
			}
		}

		return charsets;
	}

}
