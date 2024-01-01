package com.example.springs3demo.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3Service implements FileServiceImpl{

    @Value("${bucketName}")
    private String bucketName;

    private  final AmazonS3 s3;

    public S3Service(AmazonS3 s3) {
        this.s3 = s3;
    }

    @Override
    public String saveFile(MultipartFile file) {
        String Filename = generateFileName(file);
        int count = 0;
        int maxTries = 3;
        while(true) {
            try {
                File file1 = convertMultiPartToFile(file);
                PutObjectResult putObjectResult = s3.putObject(bucketName, Filename, file1);
                return putObjectResult.getContentMd5();
            } catch (IOException e) {
                if (++count == maxTries) throw new RuntimeException(e);
            }
        }

    }

    @Override
    public byte[] downloadFile(String filename) {
        S3Object object = s3.getObject(bucketName, filename);
        S3ObjectInputStream objectContent = object.getObjectContent();
        try {
            return IOUtils.toByteArray(objectContent);
        } catch (IOException e) {
            throw  new RuntimeException(e);
        }


    }

    public static String generateFileName(MultipartFile multiPart) {
        return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
    }
    //Frontend code to display the image
//    const [imageData, setImageData] = useState(null);
//
//    // Assuming your response.data contains the byte array
//    useEffect(() => {
//    const fetchImage = async () => {
//            try {
//        const response = await fetch(
//                        "http://localhost:8080/download/1668517537383.jpg"
//                );
//        const data = await response.blob();
//        const imageUrl = URL.createObjectURL(data);
//                setImageData(imageUrl);
//            } catch (error) {
//                console.error("Error fetching image:", error);
//            }
//        };
//
//        fetchImage();
//
//        // Cleanup the URL when the component unmounts
//        return () => {
//            URL.revokeObjectURL(imageData);
//        };
//    }, []);

    @Override
    public String deleteFile(String filename) {

        s3.deleteObject(bucketName,filename);
        return "File deleted";
    }

    @Override
    public List<String> listAllFiles() {

        ListObjectsV2Result listObjectsV2Result = s3.listObjectsV2(bucketName);
        return  listObjectsV2Result.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());

    }


    private File convertMultiPartToFile(MultipartFile file ) throws IOException
    {
        File convFile = new File( file.getOriginalFilename() );
        FileOutputStream fos = new FileOutputStream( convFile );
        fos.write( file.getBytes() );
        fos.close();
        return convFile;
    }
}
