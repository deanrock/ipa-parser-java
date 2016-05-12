package ipareader;
import java.io.IOException;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.dd.plist.PropertyListFormatException;



public class Reader {
	public static final void main(String... argv) throws IOException, PropertyListFormatException, ParseException, ParserConfigurationException, SAXException {
		String path = "<path to .ipa file>";
		
        IPAReader reader = new IPAReader(path);
        IPAInfo info = reader.parse();
        
        System.out.println(info.getBundleName());
 	    System.out.println(info.getRequiredDeviceCapabilities());
	}
}
