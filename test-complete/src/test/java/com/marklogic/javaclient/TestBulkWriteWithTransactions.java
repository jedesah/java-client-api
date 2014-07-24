package com.marklogic.javaclient;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.Transaction;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.document.XMLDocumentManager;
import com.marklogic.client.io.DOMHandle;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.DocumentMetadataHandle.Capability;
import com.marklogic.client.io.DocumentMetadataHandle.DocumentCollections;
import com.marklogic.client.io.DocumentMetadataHandle.DocumentPermissions;
import com.marklogic.client.io.DocumentMetadataHandle.DocumentProperties;

public class TestBulkWriteWithTransactions extends BasicJavaClientREST {

	private static final int BATCH_SIZE=100;
	private static final String DIRECTORY ="/bulkTransacton/";
	private static String dbName = "TestBulkWriteWithTransactionDB";
	private static String [] fNames = {"TestBulkWriteWithTransactionDB-1"};
	private static String restServerName = "REST-Java-Client-API-Server";
	private static int restPort = 8011;
	private  DatabaseClient client ;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("In Setup");
//		setupJavaRESTServer(dbName, fNames[0], restServerName,restPort);
//		  createRESTUser("app-user", "password","rest-writer","rest-reader"  );
//		  createRESTUserWithPermissions("usr1", "password",getPermissionNode("eval",Capability.READ),getCollectionNode("http://permission-collections/"), "rest-writer","rest-reader" );
//		  setMaintainLastModified(dbName, true);
	}
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("In tear down" );
//		tearDownJavaRESTServer(dbName, fNames, restServerName);
//		deleteRESTUser("app-user");
//		deleteRESTUser("usr1");
	}
	@Before
	public void setUp() throws Exception {
		   System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
		   System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");   
		// create new connection for each test below
		  client = DatabaseClientFactory.newClient("localhost", restPort, "usr1", "password", Authentication.DIGEST);
	}
	@After
	public void tearDown() throws Exception {
		System.out.println("Running clear script");	
    	// release client
    	client.release();
	}
	public DocumentMetadataHandle setMetadata(){
		  // create and initialize a handle on the metadata
		DocumentMetadataHandle metadataHandle = new DocumentMetadataHandle();
	    metadataHandle.getCollections().addAll("my-collection1","my-collection2");
	    metadataHandle.getPermissions().add("app-user", Capability.UPDATE, Capability.READ);
	    metadataHandle.getProperties().put("reviewed", true);
	    metadataHandle.getProperties().put("myString", "foo");
	    metadataHandle.getProperties().put("myInteger", 10);
	    metadataHandle.getProperties().put("myDecimal", 34.56678);
	    metadataHandle.getProperties().put("myCalendar", Calendar.getInstance().get(Calendar.YEAR));
	    metadataHandle.setQuality(23);
	    return	metadataHandle;
	}
	public void validateMetadata(DocumentMetadataHandle mh){

	    // get metadata values
	    DocumentProperties properties = mh.getProperties();
	    DocumentPermissions permissions = mh.getPermissions();
	    DocumentCollections collections = mh.getCollections();
	    
	    // Properties
	   // String expectedProperties = "size:5|reviewed:true|myInteger:10|myDecimal:34.56678|myCalendar:2014|myString:foo|";
	    String actualProperties = getDocumentPropertiesString(properties);
	    boolean result = actualProperties.contains("size:6|");
	    assertTrue("Document properties count", result);
	    
	    // Permissions
	    String expectedPermissions1 = "size:4|rest-reader:[READ]|eval:[READ]|app-user:[UPDATE, READ]|rest-writer:[UPDATE]|";
	    String expectedPermissions2 = "size:4|rest-reader:[READ]|eval:[READ]|app-user:[READ, UPDATE]|rest-writer:[UPDATE]|";
	    String actualPermissions = getDocumentPermissionsString(permissions);
	    System.out.println(actualPermissions);
	    if(actualPermissions.contains("[UPDATE, READ]"))
	    	assertEquals("Document permissions difference", expectedPermissions1, actualPermissions);
	    else if(actualPermissions.contains("[READ, UPDATE]"))
	    	assertEquals("Document permissions difference", expectedPermissions2, actualPermissions);
	    else
	    	assertEquals("Document permissions difference", "wrong", actualPermissions);
	    
	    // Collections 
	    String expectedCollections = "size:2|my-collection1|my-collection2|";
	    String actualCollections = getDocumentCollectionsString(collections);
	    assertEquals("Document collections difference", expectedCollections, actualCollections);
	    
	}	
	public void validateDefaultMetadata(DocumentMetadataHandle mh){

	    // get metadata values
	    DocumentPermissions permissions = mh.getPermissions();
	    DocumentCollections collections = mh.getCollections();
	  	     
	    // Permissions
	    
	    String expectedPermissions1 = "size:3|rest-reader:[READ]|eval:[READ]|rest-writer:[UPDATE]|";
	    String actualPermissions = getDocumentPermissionsString(permissions);
	   	assertEquals("Document permissions difference", expectedPermissions1, actualPermissions);
	    
	    // Collections 
	    String expectedCollections = "size:1|http://permission-collections/|";
	    String actualCollections = getDocumentCollectionsString(collections);
	    
	    assertEquals("Document collections difference", expectedCollections, actualCollections);
//	    System.out.println(actualPermissions);
	}
	public void validateRecord(DocumentRecord record,Format type) {
	       
        assertNotNull("DocumentRecord should never be null", record);
        assertNotNull("Document uri should never be null", record.getUri());
        assertTrue("Document uri should start with " + DIRECTORY, record.getUri().startsWith(DIRECTORY));
        assertEquals("All records are expected to be in same format", type, record.getFormat());
     }
	/*
	 * This test is trying to bulk insert in 
	 */
	@Test
	public void testBulkWritewithTransactions() throws Exception {
		
		int count=1;
		Transaction t1 = client.openTransaction();
	    XMLDocumentManager docMgr = client.newXMLDocumentManager();
	    HashMap<String,String> map= new HashMap<String,String>();
	    DocumentWriteSet writeset =docMgr.newWriteSet();
	    for(int i =0;i<102;i++){
	    	
		    writeset.add(DIRECTORY+"foo"+i+".xml", new DOMHandle(getDocumentContent("This is so foo"+i)));
		    map.put(DIRECTORY+"foo"+i+".xml", convertXMLDocumentToString(getDocumentContent("This is so foo"+i)));
		    if(count%BATCH_SIZE == 0){
		    	  docMgr.write(writeset,t1);
		    	  writeset = docMgr.newWriteSet();
		    	}
		      count++;
	    }
	    if(count%BATCH_SIZE > 0){
	    	docMgr.write(writeset,t1);
	 	 }
		 String uris[] = new String[102];
		 for(int i =0;i<102;i++){
		    uris[i]=DIRECTORY+"foo"+i+".xml";
		  }
		// t1.commit();
		  count=0;
		  DocumentPage page = docMgr.read(t1,uris);
		  DOMHandle dh = new DOMHandle();
		  while(page.hasNext()){
		    	DocumentRecord rec = page.next();
		    	validateRecord(rec,Format.XML);
		    	rec.getContent(dh);
		    	assertEquals("Comparing the content :",map.get(rec.getUri()),convertXMLDocumentToString(dh.get()));
		    	count++;
		  }
		  
		 assertEquals("document count", 102,count); 
	}

}
