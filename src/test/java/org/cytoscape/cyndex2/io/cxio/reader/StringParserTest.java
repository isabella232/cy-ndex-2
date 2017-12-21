package org.cytoscape.cyndex2.io.cxio.reader;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class StringParserTest {

	@Test
	public void test() throws IOException {
		StringParser sp = new StringParser("COL=name,T=string");
		
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		
		String testStr = "COL=name,T=string,K=0=Chromosome 19,V=0=35.0,K=1=CSF1R,V=1=60.0,K=2=FLT3,V=2=60.0,K=3=PTEN,V=3=60.0,K=4=GATA3,V=4=60.0,K=5=PPP2R1A,V=5=60.0,K=6=Chromosome 10,V=6=35.0,K=7=Chromosome 12,V=7=35.0,K=8=VHL,V=8=60.0,K=9=Chromosome 15,V=9=35.0,K=10=Chromosome 16,V=10=35.0,K=11=JAK3,V=11=60.0,K=12=SF3B1,V=12=60.0,K=13=Chromosome 18,V=13=35.0,K=14=SMAD2,V=14=60.0,K=15=MAP2K4,V=15=60.0,K=16=PDGFRA,V=16=60.0,K=17=CREBBP,V=17=60.0,K=18=DNMT3A,V=18=60.0,K=19=IDH2,V=19=60.0,K=20=Chromosome 7,V=20=35.0,K=21=Chromosome 5,V=21=35.0,K=22=BRAF,V=22=60.0,K=23=Chromosome 4,V=23=35.0,K=24=Chromosome 3,V=24=35.0,K=25=Chromosome 2,V=25=35.0,K=26=Chromosome 1,V=26=35.0,K=27=SMO,V=27=60.0,K=28=Chromosome 20,V=28=35.0,K=29=GNAQ,V=29=60.0,K=30=Chromosome 8,V=30=35.0,K=31=CTNNB1,V=31=60.0,K=32=FGFR2,V=32=60.0,K=33=CRLF2,V=33=60.0";
		sp = new StringParser(testStr);
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Chromosome 19");
		assertEquals (sp.get("V=0"), "35.0");	
		assertEquals (sp.get("K=33"), "CRLF2");
		assertEquals (sp.get("V=33"),"60.0");
	
		testStr = "COL=correl=ati,,on,,,T=double,L=0=#0066FF,E=0=#0066FF,G=0=#0066FF,OV=0=0.0,L=1=#0066FF,E=1=#FF3333,G=1=#FF3300,OV=1=0.0";
		sp = new StringParser(testStr);
		assertEquals (sp.get("COL"), "correl=ati,on,");
		assertEquals (sp.get("T"), "double");
		assertEquals (sp.get("L=0"), "#0066FF");
		assertEquals (sp.get("E=0"), "#0066FF");	
		assertEquals (sp.get("G=0"), "#0066FF");
		assertEquals (sp.get("OV=0"),"0.0");
		assertEquals (sp.get("L=1"), "#0066FF");
		assertEquals (sp.get("E=1"), "#FF3333");	
		assertEquals (sp.get("G=1"), "#FF3300");
		assertEquals (sp.get("OV=1"),"0.0");
		
		sp = new StringParser("COL=name,T=string,K=0=Node_1,V=0=#009933,K=1=Node_2,V=1=#003399");
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Node_1");
		assertEquals (sp.get("V=0"), "#009933");	
		assertEquals (sp.get("K=1"), "Node_2");
		assertEquals (sp.get("V=1"), "#003399");

		sp = new StringParser("COL=name,T=string,K=0=Node_1,V=0=#009933,K=1=Node=2,V=1=#003399");
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Node_1");
		assertEquals (sp.get("V=0"), "#009933");	
		assertEquals (sp.get("K=1"), "Node=2");
		assertEquals (sp.get("V=1"), "#003399");
		
		
		sp = new StringParser("COL=name,T=string,K=0=Node,,1,V=0=#009933,K=1=Node=2,V=1=#003399");
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Node,1");
		assertEquals (sp.get("V=0"), "#009933");	
		assertEquals (sp.get("K=1"), "Node=2");
		assertEquals (sp.get("V=1"), "#003399");
		
		sp = new StringParser("COL=name,T=string,K=0=Node,,1,,,V=0=#009933,K=1=Node 2,V=1=#003399");
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Node,1,");
		assertEquals (sp.get("V=0"), "#009933");	
		assertEquals (sp.get("K=1"), "Node 2");
		assertEquals (sp.get("V=1"), "#003399");
		
		sp = new StringParser("COL=name,T=string,K=0=Node,,1,,,,,V=0=#009933,K=1=Node 2,V=1=#003399");
		assertEquals (sp.get("COL"), "name");
		assertEquals (sp.get("T"), "string");
		assertEquals (sp.get("K=0"), "Node,1,,");
		assertEquals (sp.get("V=0"), "#009933");	
		assertEquals (sp.get("K=1"), "Node 2");
		assertEquals (sp.get("V=1"), "#003399");
	}

}
