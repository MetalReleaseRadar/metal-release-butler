package rocks.metaldetector.butler.service.importjob

import rocks.metaldetector.butler.model.importjob.ImportJobEntity
import rocks.metaldetector.butler.model.importjob.ImportJobRepository
import spock.lang.Specification

import static rocks.metaldetector.butler.DtoFactory.ImportJobEntityFactory
import static rocks.metaldetector.butler.model.importjob.JobState.ERROR
import static rocks.metaldetector.butler.model.importjob.JobState.RUNNING
import static rocks.metaldetector.butler.model.importjob.JobState.SUCCESSFUL
import static rocks.metaldetector.butler.model.release.ReleaseSource.METAL_ARCHIVES
import static rocks.metaldetector.butler.model.release.ReleaseSource.TIME_FOR_METAL

class ImportJobServiceTest extends Specification {

  ImportJobService underTest = new ImportJobService(\
          importJobRepository: Mock(ImportJobRepository),
          importJobTransformer: Mock(ImportJobTransformer),
          metalArchivesReleaseImporter: Mock(MetalArchivesReleaseImporter),
          timeForMetalReleaseImporter: Mock(TimeForMetalReleaseImporter)
  )

  def "findAllImportJobResults: should call import job repository"() {
    when:
    underTest.findAllImportJobResults()

    then:
    1 * underTest.importJobRepository.findAll()
  }

  def "findAllImportJobResults: should transform each job entity with job transformer"() {
    given:
    def jobEntities = [
            ImportJobEntityFactory.createImportJobEntity(),
            ImportJobEntityFactory.createImportJobEntity()
    ]
    underTest.importJobRepository.findAll() >> jobEntities

    when:
    underTest.findAllImportJobResults()

    then:
    1 * underTest.importJobTransformer.transform(jobEntities[0])

    then:
    1 * underTest.importJobTransformer.transform(jobEntities[1])
  }

  def "findAllImportJobResults: should return list of transformed import jobs"() {
    given:
    def jobEntities = [
            ImportJobEntityFactory.createImportJobEntity(),
            ImportJobEntityFactory.createImportJobEntity()
    ]
    underTest.importJobRepository.findAll() >> jobEntities

    when:
    def results = underTest.findAllImportJobResults()

    then:
    results.size() == jobEntities.size()
  }

  def "importFromExternalSources: should create a new import job, invoking 'importReleases()' and update the import job in this order"() {
    given:
    underTest.releaseImporters = [underTest.metalArchivesReleaseImporter]
    underTest.metalArchivesReleaseImporter.releaseSource >> METAL_ARCHIVES
    ImportResult importResult = new ImportResult(totalCountRequested: 10, totalCountImported: 5)
    ImportJobEntity metalArchivesImportJob = new ImportJobEntity(jobId: UUID.randomUUID())

    when:
    underTest.importFromExternalSources()

    then:
    1 * underTest.importJobRepository.save({
      assert it.jobId != null
      assert it.startTime != null
      assert it.source == METAL_ARCHIVES
    }) >> metalArchivesImportJob

    then:
    1 * underTest.metalArchivesReleaseImporter.importReleases() >> importResult

    then:
    1 * underTest.importJobRepository.save({ args ->
      assert args.jobId == metalArchivesImportJob.jobId
      assert args.totalCountRequested == importResult.totalCountRequested
      assert args.totalCountImported == importResult.totalCountImported
      assert args.state == SUCCESSFUL
      assert args.endTime
    })
  }

  def "importFromExternalSources: should handle any exception and update the import job with state ERROR"() {
    given:
    underTest.releaseImporters = [underTest.metalArchivesReleaseImporter]
    underTest.metalArchivesReleaseImporter.releaseSource >> METAL_ARCHIVES
    ImportJobEntity metalArchivesImportJob = new ImportJobEntity(jobId: UUID.randomUUID())
    underTest.importJobRepository.save(*_) >> metalArchivesImportJob
    underTest.metalArchivesReleaseImporter.importReleases() >> { throw new  RuntimeException("boom") }

    when:
    underTest.importFromExternalSources()

    then:
    1 * underTest.importJobRepository.save({ args ->
      assert args.jobId == metalArchivesImportJob.jobId
      assert args.totalCountRequested == null
      assert args.totalCountImported == null
      assert args.state == ERROR
      assert args.endTime
    })

    and:
    noExceptionThrown()
  }

  def "importFromExternalSources: should process the release importers according to the specified order"() {
    given:
    underTest.releaseImporters = [underTest.metalArchivesReleaseImporter, underTest.timeForMetalReleaseImporter]
    underTest.metalArchivesReleaseImporter.releaseSource >> METAL_ARCHIVES
    underTest.timeForMetalReleaseImporter.releaseSource >> TIME_FOR_METAL
    underTest.importJobRepository.save(*_) >> new ImportJobEntity()

    when:
    underTest.importFromExternalSources()

    then:
    1 * underTest.metalArchivesReleaseImporter.importReleases() >> new ImportResult()

    then:
    1 * underTest.timeForMetalReleaseImporter.importReleases() >> new ImportResult()
  }

  def "updateImportJob: should update the corresponding import job"() {
    given:
    def importJobEntity = new ImportJobEntity(jobId: UUID.randomUUID())
    def jobState = SUCCESSFUL
    def importResult = new ImportResult(
            totalCountRequested: 10,
            totalCountImported: 5
    )

    when:
    underTest.updateImportJob(importJobEntity, importResult, jobState)

    then:
    1 * underTest.importJobRepository.save({ args ->
      assert args.jobId == importJobEntity.jobId
      assert args.totalCountRequested == importResult.totalCountRequested
      assert args.totalCountImported == importResult.totalCountImported
      assert args.endTime
    })
  }

  def "createImportJob: should call importJobRepository when creating new import job"() {
    given:
    def givenReleaseSource = METAL_ARCHIVES

    when:
    underTest.createImportJob(givenReleaseSource)

    then:
    1 * underTest.importJobRepository.save({ args ->
      assert args.jobId
      assert args.startTime
      assert args.state == RUNNING
      assert args.source == givenReleaseSource
    })
  }

  def "createImportJob: should return created import job"() {
    given:
    def createdJobEntity = new ImportJobEntity(jobId: UUID.randomUUID())
    underTest.importJobRepository.save(*_) >> createdJobEntity

    when:
    def result = underTest.createImportJob()

    then:
    result == createdJobEntity
  }

  def "retryCoverDownload: should call all importers on retryCoverDownload"() {
    given:
    underTest.releaseImporters = [underTest.metalArchivesReleaseImporter, underTest.timeForMetalReleaseImporter]

    when:
    underTest.retryCoverDownload()

    then:
    1 * underTest.metalArchivesReleaseImporter.retryCoverDownload()
    1 * underTest.timeForMetalReleaseImporter.retryCoverDownload()
  }

  def "retryCoverDownload: should handle any exception"() {
    given:
    underTest.releaseImporters = [underTest.metalArchivesReleaseImporter]
    underTest.retryCoverDownload() >> { throw new RuntimeException("boom") }

    when:
    underTest.retryCoverDownload()

    then:
    noExceptionThrown()
  }
}
