package com.february.edsc.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.february.edsc.common.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	@Value("${cloud.aws.s3.bucket}")
	public String bucket;

	private final AmazonS3Client amazonS3Client;

	private final static List<String> IMAGE_EXTENSIONS = Arrays
		.asList(".jpg", ".jpeg", ".gif", ".png", ".img", ".tiff", ".heif");
//	private final static String TEMP_FILE_PATH = "src/main/resources/"; // local test
	private final static String TEMP_FILE_PATH = "/home/ec2-user/app/EDSC-server/src/main/resources/";

	public Optional<File> convert(MultipartFile file) throws IOException {
		File convertFile = new File(TEMP_FILE_PATH + file.getOriginalFilename());
		if (convertFile.createNewFile()) {
			try (FileOutputStream fos = new FileOutputStream(convertFile)) {
				fos.write(file.getBytes());
			}
			return Optional.of(convertFile);
		}
		throw new IllegalArgumentException(("파일 변환이 실패했습니다."));
	}

	public String upload(File uploadFile, String dirName, String id) {
		String fileName = dirName + "/" + id;
		String uploadImageUrl = putS3(uploadFile, fileName);
		deleteLocalFile(uploadFile);
		return uploadImageUrl;
	}

	public boolean isValidExtension(File uploadFile) {
		String fileName = uploadFile.getName().toLowerCase();
		String extension = "." + fileName.substring(fileName.lastIndexOf(".") + 1);
		if (!IMAGE_EXTENSIONS.contains(extension)) {
			deleteLocalFile(uploadFile);
			return false;
		}
		return true;
	}

	public void delete(String dirName, String id) {
		amazonS3Client.deleteObject(bucket, dirName + "/" + id);
	}

	private String putS3(File uploadFile, String fileName) {
		amazonS3Client.putObject(
			new PutObjectRequest(bucket, fileName, uploadFile)
				.withCannedAcl(CannedAccessControlList.PublicRead));
		return amazonS3Client.getUrl(bucket, fileName).toString();
	}

	private void deleteLocalFile(File uploadFile) {
		if (!uploadFile.delete()) {
			log.debug(ErrorMessage.FAIL_FILE_DELETE);
		}
	}
}
