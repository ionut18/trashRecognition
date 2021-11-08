package com.example.trashRecognition;

import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ImageLabelsEndpoint {

    private final CloudVisionTemplate cloudVisionTemplate;

    public ImageLabelsEndpoint(CloudVisionTemplate cloudVisionTemplate) {
        this.cloudVisionTemplate = cloudVisionTemplate;
    }

    @PostMapping("/prediction")
    public TRASH_TYPE getLabels(@RequestBody final MultipartFile file) {
        final AnnotateImageResponse response =
                this.cloudVisionTemplate.analyzeImage(file.getResource(), Feature.Type.LABEL_DETECTION);

        final Set<String> imageLabels = response.getLabelAnnotationsList().stream()
                .peek(s -> System.out.println(s.getDescription() + "   " + s.getScore()))
                .map(EntityAnnotation::getDescription)
                .collect(Collectors.toSet());

        System.out.println("------");

        return getTrashType(imageLabels);
    }

    private TRASH_TYPE getTrashType(Set<String> imageLabels) {
        if (isPlastic(imageLabels)) {
            speech("mpg123 /home/oane/Workspace/trashRecognition/src/main/resources/plastic.mp3");
            return TRASH_TYPE.PLASTIC;
        } else if (isMetal(imageLabels)) {
            speech("mpg123 /home/oane/Workspace/trashRecognition/src/main/resources/metal.mp3");
            return TRASH_TYPE.METAL;
        } else if (isPaper(imageLabels)) {
            speech("mpg123 /home/oane/Workspace/trashRecognition/src/main/resources/hartie.mp3");
            return TRASH_TYPE.PAPER;
        }
        return TRASH_TYPE.NOTHING;
    }

    private Boolean isPlastic(final Set<String> labels) {
        final Set<String> plasticLabels = Set.of("Plastic", "Plastic bottle", "Aluminium can");
        return labels.stream()
                .anyMatch(plasticLabels::contains);
    }

    private Boolean isMetal(final Set<String> labels) {
        final Set<String> glassLabels = Set.of("Aluminium", "Metal", "Aluminium can", "Tin", "Tin can", "Cylinder");
        return labels.stream()
                .anyMatch(glassLabels::contains);
    }

    private Boolean isPaper(final Set<String> labels) {
        final Set<String> paperLabels = Set.of("Paper", "Cardboard", "Packing materials", "Carton", "Wood");
        return labels.stream()
                .anyMatch(paperLabels::contains);
    }

    private void speech(final String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", command);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    enum TRASH_TYPE {
        PLASTIC,
        METAL,
        PAPER,
        NOTHING
    }
}
