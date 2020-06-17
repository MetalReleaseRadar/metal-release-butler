package rocks.metaldetector.butler.model.importjob

import groovy.transform.EqualsAndHashCode
import groovy.transform.builder.Builder
import rocks.metaldetector.butler.model.BaseEntity
import rocks.metaldetector.butler.model.release.ReleaseSource

import javax.persistence.Column
import javax.persistence.Entity
import java.time.LocalDateTime

@Entity(name = "import_jobs")
@EqualsAndHashCode(callSuper = false)
@Builder(excludes = "new") // because there is method isNew in super class
class ImportJobEntity extends BaseEntity {

  @Column(name = "job_id", nullable = false)
  UUID jobId

  @Column(name = "total_count_requested", nullable = true)
  int totalCountRequested

  @Column(name = "total_count_imported", nullable = true)
  int totalCountImported

  @Column(name = "start_time", nullable = false, columnDefinition = "timestamp")
  LocalDateTime startTime

  @Column(name = "end_time", nullable = true, columnDefinition = "timestamp")
  LocalDateTime endTime

  @Column(name = "source", nullable = false)
  ReleaseSource source

}
