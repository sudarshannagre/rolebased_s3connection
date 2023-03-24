package com.sud.s3connection.rolebased_s3connection.util;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.StsException;

@Configuration
public class RoleBasedS3Connection implements InitializingBean {

	@Value("${aws.s3.region}")
	private String clientRegion;

	@Value("${aws.s3.roleARN}")
	private String roleArn;

	@Value("${aws.s3.roleSessionName}")
	private String roleSessionName;

	@Value("${aws.s3.bucket.name}")
	private String DESTINATION_BUCKET_NAME;

	private void assumeSTSRoleForS3Connect() {

		Region region = Region.of(clientRegion);
		StsClient stsClient = StsClient.builder().region(region).build();

		try {
			software.amazon.awssdk.services.sts.model.AssumeRoleRequest roleRequest = software.amazon.awssdk.services.sts.model.AssumeRoleRequest
					.builder().roleArn(roleArn).roleSessionName(roleSessionName).build();

			AssumeRoleResponse roleResponse = stsClient.assumeRole(roleRequest);
			software.amazon.awssdk.services.sts.model.Credentials myCreds = roleResponse.credentials();
			String key = myCreds.accessKeyId();
			String secKey = myCreds.secretAccessKey();
			String secToken = myCreds.sessionToken();

			// List all objects in an Amazon S3 bucket using the temp creds.
			S3Client s3 = S3Client.builder()
					.credentialsProvider(
							StaticCredentialsProvider.create(AwsSessionCredentials.create(key, secKey, secToken)))
					.region(region).build();

			System.out.println("Created a S3Client using temp credentials.");
			System.out.println("Listing objects in " + DESTINATION_BUCKET_NAME);
			ListObjectsRequest listObjects = ListObjectsRequest.builder().bucket(DESTINATION_BUCKET_NAME).build();

			ListObjectsResponse res = s3.listObjects(listObjects);
			List<S3Object> objects = res.contents();
			for (S3Object myValue : objects) {
				System.out.println("The name of the key is " + myValue.key());
				System.out.println("The owner is " + myValue.owner());
			}

		} catch (StsException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		assumeSTSRoleForS3Connect();
	}
}
