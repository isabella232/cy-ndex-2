package org.cytoscape.cyndex2.io.cxio.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cytoscape.cyndex2.io.cxio.CxUtil;

public final class StringParser {
    private final Map<String, String> _data = new HashMap<>();
    
    private static final Pattern p0 = Pattern.compile("(?<=(^|,))([^,]|,,)*(?=(,|$))");

    private static final Pattern p = Pattern.compile("^((K|V|L|E|G|OV)=([0-9]+))=(.*)$");

    public static List<String> commaDelimitedListStringToStringList2(String list) {
      /*  if (!list.matches("^(([^,]|,,)*(,|$))+")) {
            return null;
        } */
        Matcher matcher = p0.matcher(list);
        ArrayList<String> result = new ArrayList<>();
        while (matcher.find()) {
        		String v = matcher.group();
        		if ( v.length() > 0 )
        			result.add(v);
        }
        return result;
    }

    public StringParser(final String str) throws IOException {
    	
    		//String array[] = str.split("(?<!\\\\),");
    		List<String> array = commaDelimitedListStringToStringList2(str);
        for( String n : array) {
        		if(n.startsWith("COL=")) {
        			String v = n.substring(4);
        			_data.put(CxUtil.VM_COL, v.replace(",,", ","));
        		} else if ( n.startsWith("T=")) {
        			String v = n.substring(2);
        			_data.put(CxUtil.VM_TYPE, v);       			
        		} else {
        			Matcher m = p.matcher(n);
        			if ( !m.matches())
        				throw new IOException ("Failed to parse mapping string: "+ n);
       // 			System.out.println(m.group(1));
       // 			System.out.println(m.group(4));
        			_data.put(m.group(1), m.group(4));
        		} 
        	
        	
        /*    final String[] m = n.split("=");
            if (m.length == 2) {
                if ((m[0] != null) && (m[1] != null)) {
                    _data.put(m[0], m[1]);
                }
            }
            else if (m.length == 3) {
                if ((m[0] != null) && (m[1] != null) && (m[2] != null)) {
                    _data.put(m[0] + "=" + m[1], m[2]);
                }
            } */
        }
    }

    public final String get(final String key) {
        return _data.get(key);
    }

}
