package com.rs.app.controller;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.plaf.multi.MultiButtonUI;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.rs.app.model.DicomInput;

@RestController
@RequestMapping(value = DICOMController.DICOMCONTROLLER_BASE_URI)
public class DICOMController {
	protected static final String DICOMCONTROLLER_BASE_URI = "/dicom";
	private static final Logger LOG = LoggerFactory.getLogger(DICOMController.class);

	@PostMapping("/upload")
	public String uploadDicomFile(@RequestParam("file") MultipartFile multipartFile) throws IOException {
		Properties props = new Properties();
		DicomInput dicomInput = readDICOMObjectAndStoreInPropsFile(multipartFile);
		InputStream is = new FileInputStream(dicomInput.getName() + ".properties");
		// ByteArrayInputStream bis = new ByteArrayInputStream(is.readAllBytes());
		String fileName = "";
		// System.out.println("All bytes length? " + is.readAllBytes().length); //92
		fileName = is.available() + "_" + dicomInput.getName() + ".dcm";
		System.out.println("fileName? " + fileName);
		URL url = new URL("http://localhost:4040/servlet-fileupload-1/FileUploadServlet");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.setConnectTimeout(30000);
		OutputStream os = conn.getOutputStream();
		ZipOutputStream zos = new ZipOutputStream(os);
		zos.putNextEntry(new ZipEntry(fileName));
		byte[] buff = new byte[8 * 1024];
		int read = 0;
		//Write properties file content to stream
		BufferedInputStream bis1 = new BufferedInputStream(is);
		while ((read = bis1.read(buff)) != -1) {
			System.out.println("Uploading props file");
			zos.write(buff, 0, read);
		}
		bis1.close();
		//Write Dicom file to stream
		is = multipartFile.getInputStream();
		BufferedInputStream bis = new BufferedInputStream(is);
		while ((read = bis.read(buff)) != -1) {
			System.out.println("Uploading dicom file");
			zos.write(buff, 0, read);
		}	
		zos.closeEntry();
		//conn.connect();
		int responseCode = conn.getResponseCode();
		String responseMessage = conn.getResponseMessage();
		System.out.println("ResponseCode? " + responseCode);
		System.out.println("Response Message? " + responseMessage);
		
		System.out.println("Zip file writen success fully");
		is.close();
		return fileName;
	}

	@PostMapping("/read")
	public DicomInput readDICOMObjectAndStoreInPropsFile(@RequestParam("file") MultipartFile file) throws IOException {
		// File file = new File(dicomFile);

		try {
			DicomObject dicomObject = readDicom(file.getInputStream(), -1);
			// return JsonUtil.convertJavaObjectToJson(dicomObject, true);
			Iterator<DicomElement> datasetIterator = dicomObject.datasetIterator();
			// datasetIterator.
			while (datasetIterator.hasNext()) {
				System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
				DicomElement dicomElement = datasetIterator.next();
				System.out.println("VR: " + dicomElement.vr());
				System.out.println("CountItems: " + dicomElement.countItems());
				System.out.println("Length: " + dicomElement.length());
				System.out.println("isTag field: " + isTagField(dicomElement.toString()));
				System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
			}
			String PN = dicomObject.getString(Tag.PatientName);
			String AGE = dicomObject.getString(Tag.PatientAge);
			String ADDRS = dicomObject.getString(Tag.PatientAddress);
			File propsFile = new File(PN + ".properties");
			FileOutputStream fos = new FileOutputStream(propsFile);
			Properties props = new Properties();
			props.setProperty("PatientName", PN);
			props.setProperty("PatientAge", AGE);
			props.setProperty("PatientAddress", ADDRS);
			props.store(fos, null);
			DicomInput di = new DicomInput();
			di.setName(PN);
			di.setAge(Integer.parseInt(AGE));
			di.setAddress(ADDRS);
			return di;
		} finally {

		}
	}

	public static boolean isTagField(String name) {
		try {
			Tag.forName(name);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	@PostMapping("/create")
	public ResponseEntity<Void> createDicom(@RequestBody DicomInput dicomInput) throws Exception {
		Properties props = new Properties();
		props.setProperty("PatientName", dicomInput.getName());
		props.setProperty("PatientAge", String.valueOf(dicomInput.getAge()));
		props.setProperty("PatientAddress", dicomInput.getAddress());
		createDicomImage(props);

		return ResponseEntity.noContent().build();
	}

	public DicomObject readDicom(InputStream in, int maxTag) throws IOException {
		BufferedInputStream bin = new BufferedInputStream(in);
		DicomInputStream din = new DicomInputStream(bin);

		if (maxTag > 0)
			din.setHandler(new StopTagInputHandler(maxTag + 1));

		DicomObject dcm = din.readDicomObject();

		return dcm;
	}

	public String createDicomImage(Properties props) throws Exception {
		LOG.info("Creating DICOM Image");
		DicomObject dicomObject = new BasicDicomObject(1000);
		UUID uuid = UUID.randomUUID();
		dicomObject.putString(Tag.TransferSyntaxUID, VR.UI, uuid.toString());
		dicomObject.putString(Tag.ImageType, VR.CS, "DICOM Image");
		if (props.containsKey("PatientName")) {
			dicomObject.putString(Tag.PatientName, VR.PN, props.getProperty("PatientName"));
		}
		if (props.containsKey("PatientAge")) {
			dicomObject.putString(Tag.PatientAge, VR.AS, props.getProperty("PatientAge"));
		}
		if (props.containsKey("PatientAddress")) {
			dicomObject.putString(Tag.PatientAddress, VR.LO, props.getProperty("PatientAddress"));
		}
		try {
			writeDicom(dicomObject, "ramesh.dcm");
		} catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			throw ex;
		}
		return "DICOM Object created successfully";
	}

	private void writeDicom(DicomObject dicomObject, String targetDicomFile) throws IOException {
		LOG.info("Writing dicom object to {}", targetDicomFile);
		File file = new File(targetDicomFile);
		try (FileOutputStream fos = new FileOutputStream(file);
				DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(fos))) {
			dos.writeDicomFile(dicomObject);
		} catch (IOException ioex) {
			throw ioex;
		}

	}
}
