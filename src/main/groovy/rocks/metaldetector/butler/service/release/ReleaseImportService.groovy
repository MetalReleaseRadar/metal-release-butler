package rocks.metaldetector.butler.service.release

import rocks.metaldetector.butler.web.dto.ReleaseImportResponse

interface ReleaseImportService {

  ReleaseImportResponse importReleases()

}