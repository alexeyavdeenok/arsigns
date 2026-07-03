package com.example.feature_cv.di

/**
 * DI-граф модуля feature-cv. Единственное место, где нужно поменять биндинг
 * при замене реализации (например SignTracker -> ByteTrackSignTracker).
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class CvModule {
//
//     TODO: @Binds abstract fun bindCvLayerApi(impl: CvLayerApiImpl): com.example.domain.api.CvLayerApi
//
//     TODO: @Binds abstract fun bindSignTracker(impl: IouSignTracker): SignTracker
//           ^ поменять на ByteTrackSignTracker, когда будет готов более продвинутый трекер
// }
