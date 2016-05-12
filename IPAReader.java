package ipareader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;


public class IPAReader {
	private String fileName;

	public IPAReader(String fileName) {
		this.fileName = fileName;
	}

	public String getLastIconFileName(NSDictionary dict, String identifier) {
		NSDictionary primaryIcon = (NSDictionary) dict.get(identifier);
		NSDictionary iconFiles = (NSDictionary) primaryIcon.get("CFBundlePrimaryIcon");
		NSObject [] files = ((NSArray) iconFiles.get("CFBundleIconFiles")).getArray();

		String name = null;

		for(NSObject file : files) {
			name = file.toString();
		}

		return name;
	}

	public IPAInfo parse() throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException {
		IPAInfo info = new IPAInfo();

		File f = new File(this.fileName);
		info.setFileSize(f.length());

		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(this.fileName));
		ZipEntry entry = zipIn.getNextEntry();

		while (entry != null) {
			if (entry.getName().endsWith(".app/embedded.mobileprovision")) {
				ByteArrayOutputStream stream = readFileToMemory(zipIn);
				info.setMobileProvisionFile(stream.toByteArray());
				String plist = getPlistFromMobileProvisionFile(stream);

				if (plist != null) {
					NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(plist.getBytes());
					info.setProvisioningProfileCreationDate(rootDict.get("CreationDate").toString());
					info.setProvisioningProfileExpirationDate(rootDict.get("ExpirationDate").toString());
					info.setProvisioningProfileName(rootDict.get("Name").toString());
					info.setTeamIdentifier(rootDict.get("TeamIdentifier").toString());
					info.setTeamName(rootDict.get("TeamName").toString());

					NSObject[] devices = ((NSArray) rootDict.get("ProvisionedDevices")).getArray();

					List<String>list = new ArrayList<String>();
					for (NSObject device : devices) {
						list.add(device.toString());
					}
					info.setProvisioningProfileDevices(list);
				}else{
					return null;
				}
			}else if(entry.getName().endsWith(".app/Info.plist")) {
				info.setInfoPlistFile(readFileToMemory(zipIn).toByteArray());
				NSDictionary rootDict = (NSDictionary)PropertyListParser.parse(info.getInfoPlistFile());

				info.setMinimumOSVersion(rootDict.get("MinimumOSVersion").toString());
				info.setBundleName(rootDict.get("CFBundleName").toString());
				info.setBundleVersionString(rootDict.get("CFBundleShortVersionString").toString());
				info.setBundleIdentifier(rootDict.get("CFBundleIdentifier").toString());
				info.setBuildNumber(rootDict.get("CFBundleVersion").toString());
				info.setPlatformVersion(rootDict.get("DTPlatformVersion").toString());

				if (rootDict.containsKey("UIRequiredDeviceCapabilities")) {
					NSObject[] o = ((NSArray) rootDict.get("UIRequiredDeviceCapabilities")).getArray();

					if (o.length > 0) {
						info.setRequiredDeviceCapabilities(o[0].toString());
					}
				}

				if (rootDict.containsKey("CFBundleIcons")) {
					info.setiPhoneSupport(true);

					info.setBundleIconFileName(this.getLastIconFileName(rootDict, "CFBundleIcons"));
				}else{
					info.setiPhoneSupport(false);
				}

				if (rootDict.containsKey("CFBundleIcons~ipad")) {
					info.setiPadSupport(true);

					info.setBundleIconFileName(this.getLastIconFileName(rootDict, "CFBundleIcons~ipad"));
				}else{
					info.setiPadSupport(false);
				}
			}

			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();

		if (info.getBundleIconFileName() != null) {
			zipIn = new ZipInputStream(new FileInputStream(this.fileName));
			entry = zipIn.getNextEntry();

			while (entry != null) {
				if (entry.getName().endsWith("/"+info.getBundleIconFileName())) {
					info.setBundleIcon(readFileToMemory(zipIn).toByteArray());
				}
				
				zipIn.closeEntry();
				entry = zipIn.getNextEntry();
			}
			zipIn.close();
			
		}

		if (info.getBundleIdentifier() != null) {
			return info;
		}

		return null;
	}

	public String getPlistFromMobileProvisionFile(ByteArrayOutputStream stream) throws UnsupportedEncodingException {
		String s = stream.toString("UTF-8");

		int i = s.indexOf("<plist version=\"1.0\">");

		if (i >= 0) {
			s = s.substring(i);

			i = s.indexOf("</plist>");

			if (i >= 0) {
				String plist = s.substring(0, i+"</plist>".length());
				return plist;
			}
		}

		return null;
	}

	public ByteArrayOutputStream readFileToMemory(ZipInputStream zipIn) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] bytesIn = new byte[4096];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();

		return bos;
	}
}
