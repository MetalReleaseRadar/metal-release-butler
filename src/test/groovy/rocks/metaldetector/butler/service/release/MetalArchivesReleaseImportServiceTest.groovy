package rocks.metaldetector.butler.service.release

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import rocks.metaldetector.butler.model.release.ReleaseEntity
import rocks.metaldetector.butler.model.release.ReleaseRepository
import rocks.metaldetector.butler.service.converter.MetalArchivesReleaseEntityConverter
import rocks.metaldetector.butler.supplier.metalarchives.MetalArchivesRestClient
import rocks.metaldetector.butler.web.dto.ReleaseImportResponse
import spock.lang.Specification
import spock.lang.Unroll

import java.time.LocalDate

import static rocks.metaldetector.butler.DtoFactory.ReleaseEntityFactory.createReleaseEntity

class MetalArchivesReleaseImportServiceTest extends Specification {

  MetalArchivesReleaseImportService underTest = new MetalArchivesReleaseImportService(
      releaseRepository: Mock(ReleaseRepository),
      restClient: Mock(MetalArchivesRestClient),
      releaseEntityConverter: Mock(MetalArchivesReleaseEntityConverter),
      releaseEntityPersistenceThreadPool: Mock(ThreadPoolTaskExecutor)
  )

  def "rest client is called once on import"() {
    when:
    underTest.importReleases()

    then:
    1 * underTest.restClient.requestReleases() >> []
  }

  @Unroll
  "release converter is called for every response from rest template"() {
    given:
    underTest.restClient.requestReleases() >> releases

    when:
    underTest.importReleases()

    then:
    releases.size() * underTest.releaseEntityConverter.convert(_) >> [new ReleaseEntity()]

    where:
    releases << [
            [],
            [new String[0], new String[0]]
    ]
  }

  def "new releases are submitted to persistence thread pool"() {
    given:
    underTest.restClient.requestReleases() >> [new String[0]]
    ReleaseEntity releaseEntity1 = createReleaseEntity("Metallica", LocalDate.now())
    ReleaseEntity releaseEntity2 = createReleaseEntity("Slayer", LocalDate.now())
    underTest.releaseEntityConverter.convert(_) >> [releaseEntity1, releaseEntity2]

    when:
    underTest.importReleases()

    then:
    2 * underTest.releaseEntityPersistenceThreadPool.submit(*_)
  }

  def "ReleaseEntity, CoverService and ReleaseRepository is passed to each created PersistReleaseEntityTask"() {
    given:
    underTest.restClient.requestReleases() >> [new String[0]]
    ReleaseEntity releaseEntity = createReleaseEntity("Metallica", LocalDate.now())
    underTest.releaseEntityConverter.convert(_) >> [releaseEntity]

    when:
    underTest.importReleases()

    then:
    1 * underTest.releaseEntityPersistenceThreadPool.submit({ args ->
      assert args.releaseEntity == releaseEntity
      assert args.coverService == underTest.coverService
      assert args.releaseRepository == underTest.releaseRepository
    })
  }

  def "existing releases are not submitted to persistence thread pool"() {
    given:
    underTest.restClient.requestReleases() >> [new String[0]]
    underTest.releaseEntityConverter.convert(_) >> [new ReleaseEntity()]

    when:
    underTest.importReleases()

    then:
    1 * underTest.releaseRepository.existsByArtistAndAlbumTitleAndReleaseDate(*_) >> true

    and:
    0 * underTest.releaseEntityPersistenceThreadPool.submit(_)
  }

  def "should return a summary about how much releases are requested and how much releases are imported"() {
    given:
    underTest.restClient.requestReleases() >> [new String[0], new String[0]]
    underTest.releaseEntityConverter.convert(_) >> [
            createReleaseEntity("Metallica", LocalDate.now())
    ]
    underTest.releaseRepository.existsByArtistAndAlbumTitleAndReleaseDate(*_) >>> [true, false]

    when:
    def response = underTest.importReleases()

    then:
    response == new ReleaseImportResponse(
            totalCountRequested: 2,
            totalCountImported: 1
    )
  }
}
