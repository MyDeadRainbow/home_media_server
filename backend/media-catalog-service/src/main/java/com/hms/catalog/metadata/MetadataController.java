package com.hms.catalog.metadata;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hms.catalog.TaskExecutor;
import com.hms.shared.media.Episode;
import com.hms.shared.media.Season;
import com.hms.shared.media.Series;
import com.hms.shared.media.metadata.MetaData;
import com.hms.shared.media.metadata.MetaDataStatus;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final Logger LOG = LoggerFactory.getLogger(MetadataController.class);
    private final TaskExecutor taskExecutor;

    public MetadataController(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/update/{metaDataId}")
    public ResponseEntity<MetaData> updateMetaData(@PathVariable String metaDataId,
            @RequestBody MetaData entity) {
        try {
            MetaData oldMetaData = new MetaData.Dao().get(metaDataId);
            if (oldMetaData == null) {
                return ResponseEntity.notFound().build();
            }
            MetaData updatedMetaData = oldMetaData
                    .withAirDate(entity.airDate())
                    .withPlotSummary(entity.plotSummary())
                    .withRating(entity.rating())
                    .withStatus(entity.status())
                    .withMessage(entity.message())
                    .withTitle(entity.title());
            new MetaData.Dao().update(updatedMetaData);


            return ResponseEntity.ok(updatedMetaData);
        } catch (SQLException e) {
            LOG.error("Error while updating metadata", e);
        }
        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/requestSearch/{metaDataId}")
    public ResponseEntity<String> postMethodName(@PathVariable String metaDataId) {
        try {
            MetaData metaData = new MetaData.Dao().get(metaDataId);
            if (metaData == null) {
                return ResponseEntity.notFound().build();
            }
            new MetaData.Dao().update(metaData.withStatus(MetaDataStatus.PENDING));
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(metaDataId);
    }

}
