package org.smartregister.fhircore.engine.ui.appsetting

open class AppException : Exception() {}
class InternetConnectionException: AppException() {}
class ServerException: AppException() {}
class ConfigurationErrorException: AppException() {}

