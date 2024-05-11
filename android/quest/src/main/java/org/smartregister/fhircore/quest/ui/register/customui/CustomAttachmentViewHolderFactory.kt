package org.smartregister.fhircore.quest.ui.register.customui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.datacapture.extensions.MimeType
import com.google.android.fhir.datacapture.extensions.hasMimeType
import com.google.android.fhir.datacapture.extensions.hasMimeTypeOnly
import com.google.android.fhir.datacapture.extensions.mimeTypes
import com.google.android.fhir.datacapture.extensions.tryUnwrapContext
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.NotValidated
import com.google.android.fhir.datacapture.validation.Valid
import com.google.android.fhir.datacapture.validation.ValidationResult
import com.google.android.fhir.datacapture.views.HeaderView
import com.google.android.fhir.datacapture.views.QuestionnaireViewItem
import com.google.android.fhir.datacapture.views.attachment.CameraLauncherFragment
import com.google.android.fhir.datacapture.views.attachment.OpenDocumentLauncherFragment
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemViewHolderFactory
import com.google.android.fhir.logicalId
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.ResourceType
import org.hl7.fhir.r4.model.StringType
import org.smartregister.fhircore.quest.BuildConfig
import org.smartregister.fhircore.quest.R
import java.io.File
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

internal object CustomAttachmentViewHolderFactory : QuestionnaireItemViewHolderFactory(R.layout.custom_attachment_view_item) {

  override fun getQuestionnaireItemViewHolderDelegate() =
    object : QuestionnaireItemViewHolderDelegate {
      override lateinit var questionnaireViewItem: QuestionnaireViewItem
      private lateinit var header: HeaderView
      private lateinit var errorTextView: TextView
      private lateinit var takePhotoButton: Button
      private lateinit var uploadPhotoButton: Button
      private lateinit var uploadAudioButton: Button
      private lateinit var uploadVideoButton: Button
      private lateinit var uploadDocumentButton: Button
      private lateinit var uploadFileButton: Button
      private lateinit var divider: MaterialDivider
      private lateinit var labelUploaded: TextView
      private lateinit var photoPreview: ConstraintLayout
      private lateinit var photoThumbnail: ImageView
      private lateinit var photoTitle: TextView
      private lateinit var photoDeleteButton: Button
      private lateinit var photoDeleteButton2: ImageView
      private lateinit var filePreview: ConstraintLayout
      private lateinit var fileIcon: ImageView
      private lateinit var fileTitle: TextView
      private lateinit var fileDeleteButton: Button
      private lateinit var context: AppCompatActivity
      private lateinit var fhirEngine: FhirEngine

      override fun init(itemView: View) {
        header = itemView.findViewById(R.id.header)
        errorTextView = itemView.findViewById(R.id.error)
        takePhotoButton = itemView.findViewById(R.id.take_photo)
        uploadPhotoButton = itemView.findViewById(R.id.upload_photo)
        uploadAudioButton = itemView.findViewById(R.id.upload_audio)
        uploadVideoButton = itemView.findViewById(R.id.upload_video)
        uploadDocumentButton = itemView.findViewById(R.id.upload_document)
        uploadFileButton = itemView.findViewById(R.id.upload_file)
        divider = itemView.findViewById(R.id.divider)
        labelUploaded = itemView.findViewById(R.id.label_uploaded)
        photoPreview = itemView.findViewById(R.id.photo_preview)
        photoThumbnail = itemView.findViewById(R.id.photo_thumbnail)
        photoTitle = itemView.findViewById(R.id.photo_title)
        photoDeleteButton = itemView.findViewById(R.id.photo_delete)
        photoDeleteButton2 = itemView.findViewById(R.id.photo_delete2)
        filePreview = itemView.findViewById(R.id.file_preview)
        fileIcon = itemView.findViewById(R.id.file_icon)
        fileTitle = itemView.findViewById(R.id.file_title)
        fileDeleteButton = itemView.findViewById(R.id.file_delete)
        context = itemView.context.tryUnwrapContext()!!
        fhirEngine = FhirEngineProvider.getInstance(context.applicationContext)

      }

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        this.questionnaireViewItem = questionnaireViewItem
        header.bind(questionnaireViewItem)
        header.showRequiredOrOptionalTextInHeaderView(questionnaireViewItem)
        val questionnaireItem = questionnaireViewItem.questionnaireItem
        displayOrClearInitialPreview()
        displayTakePhotoButton(questionnaireItem)
        displayUploadButton(questionnaireItem)
        takePhotoButton.setOnClickListener { view -> onTakePhotoClicked(view, questionnaireItem) }
        uploadPhotoButton.setOnClickListener { view -> onUploadClicked(view, questionnaireItem) }
        uploadAudioButton.setOnClickListener { view -> onUploadClicked(view, questionnaireItem) }
        uploadVideoButton.setOnClickListener { view -> onUploadClicked(view, questionnaireItem) }
        uploadDocumentButton.setOnClickListener { view -> onUploadClicked(view, questionnaireItem) }
        uploadFileButton.setOnClickListener { view -> onUploadClicked(view, questionnaireItem) }
        photoDeleteButton.setOnClickListener { view -> onDeleteClicked(view) }
        photoDeleteButton2.setOnClickListener { view -> onDeleteClicked(view) }
        fileDeleteButton.setOnClickListener { view -> onDeleteClicked(view) }
        displayValidationResult(questionnaireViewItem.validationResult)

        displayAttachmentPreview(questionnaireViewItem)
      }

      private fun displayValidationResult(validationResult: ValidationResult) {
        when (validationResult) {
          is NotValidated,
          Valid,
          -> errorTextView.visibility = View.GONE

          is Invalid -> {
            errorTextView.text = validationResult.getSingleStringValidationMessage()
            errorTextView.visibility = View.VISIBLE
          }
        }
      }

      override fun setReadOnly(isReadOnly: Boolean) {
        takePhotoButton.isEnabled = !isReadOnly
        uploadPhotoButton.isEnabled = !isReadOnly
        uploadAudioButton.isEnabled = !isReadOnly
        uploadVideoButton.isEnabled = !isReadOnly
        uploadDocumentButton.isEnabled = !isReadOnly
        uploadFileButton.isEnabled = !isReadOnly
        photoDeleteButton.isEnabled = !isReadOnly
        fileDeleteButton.isEnabled = !isReadOnly
      }

      private fun displayOrClearInitialPreview() {
        val answer = questionnaireViewItem.answers.firstOrNull()

        // Clear preview if there is no answer to prevent showing old previews in views that have
        // been recycled.
        if (answer == null) {
          clearPhotoPreview()
          clearFilePreview()
          return
        }

        answer.valueAttachment?.let { attachment ->
          displayPreview(
            attachmentType = getMimeType(attachment.contentType),
            attachmentTitle = attachment.title,
            attachmentByteArray = attachment.data,
          )
        }
      }

      private fun displayTakePhotoButton(questionnaireItem: Questionnaire.QuestionnaireItemComponent) {
        if (questionnaireItem.hasMimeType(MimeType.IMAGE.value)) {
          takePhotoButton.visibility = View.VISIBLE
        }
      }

      private fun displayUploadButton(questionnaireItem: Questionnaire.QuestionnaireItemComponent) {
        when {
          questionnaireItem.hasMimeTypeOnly(MimeType.AUDIO.value) -> {
            uploadAudioButton.visibility = View.VISIBLE
          }

          questionnaireItem.hasMimeTypeOnly(MimeType.DOCUMENT.value) -> {
            uploadDocumentButton.visibility = View.VISIBLE
          }

          questionnaireItem.hasMimeTypeOnly(MimeType.IMAGE.value) -> {
            // NOOP
          }
          questionnaireItem.hasMimeTypeOnly(MimeType.VIDEO.value) -> {
            // NOOP
          }

          else -> {
            uploadFileButton.visibility = View.VISIBLE
          }
        }
      }

      private fun displayAttachmentPreview(questionnaireViewItem: QuestionnaireViewItem) {
        // Check if the answer contains an attachment
        val answer = questionnaireViewItem.answers.firstOrNull()
        answer?.valueAttachment?.let { attachment ->
          // Determine the attachment type and display preview accordingly
          when (getMimeType(attachment.contentType)) {
            MimeType.IMAGE.value -> {
              // If it's an image attachment, display the preview
              displayImagePreview(attachment)
            }
            // Handle other attachment types if needed
            else -> {
              // Clear any existing preview for non-image attachments
              clearAttachmentPreview()
            }
          }
        } ?: run {
          // If there's no attachment, clear any existing preview
          clearAttachmentPreview()
        }
      }

      private fun displayImagePreview(attachment: Attachment) {
        // Display image preview logic
        val attachmentTitle = attachment.title ?: ""
        val attachmentUri = getFileUri(attachment.title ?: "")
        loadPhotoPreview(attachmentUri, attachmentTitle)
      }

      fun getFileUri(imageFileName : String): Uri{
        imageFileName.isNotEmpty().let {
          return Uri.parse(IMAGE_CACHE_BASE_URI + imageFileName)
        }
      }

      private fun clearAttachmentPreview() {
        // Clear attachment preview logic
        photoPreview.visibility = View.GONE
        Glide.with(context).clear(photoThumbnail)
        photoTitle.text = ""
      }

      private fun onTakePhotoClicked(view: View, questionnaireItem: Questionnaire.QuestionnaireItemComponent) {
        val file = File.createTempFile("IMG_", ".jpeg", context.cacheDir)
        val attachmentUri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.supportFragmentManager.setFragmentResultListener(
          CameraLauncherFragment.CAMERA_RESULT_KEY,
          context,
        ) { _, result ->
          val isSaved = result.getBoolean(CameraLauncherFragment.CAMERA_RESULT_KEY)
          if (!isSaved) return@setFragmentResultListener

          if (questionnaireItem.isGivenSizeOverLimit(file.length().toBigDecimal())) {
            displayError(
              R.string.max_size_image_above_limit_validation_error_msg,
              questionnaireItem.maxSizeInMiBs,
            )
            displaySnackbar(view, R.string.upload_failed)
            file.delete()
            return@setFragmentResultListener
          }

          val attachmentMimeTypeWithSubType = context.getMimeTypeFromUri(attachmentUri)
          val attachmentMimeType = getMimeType(attachmentMimeTypeWithSubType)
          if (!questionnaireItem.hasMimeType(attachmentMimeType)) {
            displayError(R.string.mime_type_wrong_media_format_validation_error_msg)
            displaySnackbar(view, R.string.upload_failed)
            file.delete()
            return@setFragmentResultListener
          }

          // Create a document reference to store the file later and use the document ref
          // permanent link in attachment url
          val doc = createDocumentReference(attachmentUri, attachmentMimeTypeWithSubType)
          val answer =
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                Attachment().apply {
                  contentType = attachmentMimeTypeWithSubType
                  url = doc.url
                  title = file.name
                  creation = Date()
                }
            }

          context.lifecycleScope.launch {
            FhirEngineProvider.getInstance(context.applicationContext).create(doc)
            questionnaireViewItem.setAnswer(answer)
            divider.visibility = View.VISIBLE
            labelUploaded.visibility = View.VISIBLE
            displayPreview(
              attachmentType = attachmentMimeType,
              attachmentTitle = file.name,
              attachmentUri = attachmentUri,
            )
            displaySnackbarOnUpload(view, attachmentMimeType)
          }
        }

        CameraLauncherFragment()
          .apply { arguments = bundleOf(EXTRA_SAVED_PHOTO_URI_KEY to attachmentUri) }
          .show(context.supportFragmentManager, CustomAttachmentViewHolderFactory::class.java.simpleName)
      }

      private fun onUploadClicked(view: View, questionnaireItem: Questionnaire.QuestionnaireItemComponent) {
        context.supportFragmentManager.setFragmentResultListener(
          OpenDocumentLauncherFragment.OPEN_DOCUMENT_RESULT_KEY,
          context,
        ) { _, result ->
          val attachmentUri =
            (result.get(OpenDocumentLauncherFragment.OPEN_DOCUMENT_RESULT_KEY)
              ?: return@setFragmentResultListener)
              as Uri

          val attachmentByteArray = context.readBytesFromUri(attachmentUri)
          if (questionnaireItem.isGivenSizeOverLimit(attachmentByteArray.size.toBigDecimal())) {
            displayError(
              R.string.max_size_file_above_limit_validation_error_msg,
              questionnaireItem.maxSizeInMiBs,
            )
            displaySnackbar(view, R.string.upload_failed)
            return@setFragmentResultListener
          }

          val attachmentMimeTypeWithSubType = context.getMimeTypeFromUri(attachmentUri)
          val attachmentMimeType = getMimeType(attachmentMimeTypeWithSubType)
          if (!questionnaireItem.hasMimeType(attachmentMimeType)) {
            displayError(R.string.mime_type_wrong_media_format_validation_error_msg)
            displaySnackbar(view, R.string.upload_failed)
            return@setFragmentResultListener
          }

          val attachmentTitle = getFileName(attachmentUri)
          // Create a document reference to store the file later and use the document ref
          // permanent link in attachment url
          val doc = createDocumentReference(attachmentUri, attachmentMimeTypeWithSubType)
          val answer =
            QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
              value =
                Attachment().apply {
                  contentType = attachmentMimeTypeWithSubType
                  url = doc.url
                  title = attachmentTitle
                  creation = Date()
                }
            }
          context.lifecycleScope.launch {
            FhirEngineProvider.getInstance(context.applicationContext).create(doc)
            questionnaireViewItem.setAnswer(answer)

            divider.visibility = View.VISIBLE
            labelUploaded.visibility = View.VISIBLE
            displayPreview(
              attachmentType = attachmentMimeType,
              attachmentTitle = attachmentTitle,
              attachmentUri = attachmentUri,
            )
            displaySnackbarOnUpload(view, attachmentMimeType)
          }
        }

        OpenDocumentLauncherFragment()
          .apply {
            arguments = bundleOf(EXTRA_MIME_TYPE_KEY to questionnaireItem.mimeTypes.toTypedArray())
          }
          .show(context.supportFragmentManager, CustomAttachmentViewHolderFactory::class.java.simpleName)
      }

      private fun displayPreview(
        attachmentType: String,
        attachmentTitle: String,
        attachmentByteArray: ByteArray? = null,
        attachmentUri: Uri? = null,
      ) {
        when (attachmentType) {
          MimeType.AUDIO.value -> {
            loadFilePreview(com.google.android.fhir.datacapture.R.drawable.ic_audio_file, attachmentTitle)
            clearPhotoPreview()
          }

          MimeType.DOCUMENT.value -> {
            loadFilePreview(com.google.android.fhir.datacapture.R.drawable.ic_document_file, attachmentTitle)
            clearPhotoPreview()
          }

          MimeType.IMAGE.value -> {
            if (attachmentByteArray != null) {
              loadPhotoPreview(attachmentByteArray, attachmentTitle)
            } else if (attachmentUri != null) {
              loadPhotoPreview(attachmentUri, attachmentTitle)
            }
            clearFilePreview()
          }

          MimeType.VIDEO.value -> {
            loadFilePreview(com.google.android.fhir.datacapture.R.drawable.ic_video_file, attachmentTitle)
            clearPhotoPreview()
          }
        }
      }

      private fun loadFilePreview(@DrawableRes iconResource: Int, title: String) {
        filePreview.visibility = View.VISIBLE
        Glide.with(context).load(iconResource).into(fileIcon)
        fileTitle.text = title
      }

      private fun clearFilePreview() {
        filePreview.visibility = View.GONE
        Glide.with(context).clear(fileIcon)
        fileTitle.text = ""
      }

      private fun loadPhotoPreview(byteArray: ByteArray, title: String) {
        photoPreview.visibility = View.VISIBLE
        Glide.with(context).load(byteArray).into(photoThumbnail)
        photoTitle.text = title
      }

      private fun loadPhotoPreview(uri: Uri, title: String) {
        photoPreview.visibility = View.VISIBLE
        Glide.with(context).load(uri).into(photoThumbnail)
        photoTitle.text = title
      }

      private fun clearPhotoPreview() {
        photoPreview.visibility = View.GONE
        Glide.with(context).clear(photoThumbnail)
        photoTitle.text = ""
      }

      private fun onDeleteClicked(view: View) {
        context.lifecycleScope.launch {
          questionnaireViewItem.clearAnswer()
          divider.visibility = View.GONE
          labelUploaded.visibility = View.GONE
          clearPhotoPreview()
          clearFilePreview()
          displaySnackbarOnDelete(
            view,
            getMimeType(questionnaireViewItem.answers.first().valueAttachment.contentType),
          )
        }
      }

      private fun displaySnackbar(view: View, @StringRes textResource: Int) {
        Snackbar.make(view, context.getString(textResource), Snackbar.LENGTH_SHORT).show()
      }

      private fun displaySnackbarOnUpload(view: View, attachmentType: String) {
        when (attachmentType) {
          MimeType.AUDIO.value -> {
            displaySnackbar(view, R.string.audio_uploaded)
          }

          MimeType.DOCUMENT.value -> {
            displaySnackbar(view, com.google.android.fhir.datacapture.R.string.file_uploaded)
          }

          MimeType.IMAGE.value -> {
            displaySnackbar(view, com.google.android.fhir.datacapture.R.string.image_uploaded)
          }

          MimeType.VIDEO.value -> {
            displaySnackbar(view, R.string.video_uploaded)
          }
        }
      }

      private fun displaySnackbarOnDelete(view: View, attachmentType: String) {
        when (attachmentType) {
          MimeType.AUDIO.value -> {
            displaySnackbar(view, R.string.audio_deleted)
          }

          MimeType.DOCUMENT.value -> {
            displaySnackbar(view, com.google.android.fhir.datacapture.R.string.file_deleted)
          }

          MimeType.IMAGE.value -> {
            displaySnackbar(view, com.google.android.fhir.datacapture.R.string.image_deleted)
          }

          MimeType.VIDEO.value -> {
            displaySnackbar(view, R.string.video_deleted)
          }
        }
      }

      private fun displayError(@StringRes textResource: Int) {
        displayValidationResult(
          Invalid(
            listOf(
              context.getString(
                textResource,
              ),
            ),
          ),
        )
      }

      private fun displayError(@StringRes textResource: Int, vararg formatArgs: Any?) {
        displayValidationResult(Invalid(listOf(context.getString(textResource, *formatArgs))))
      }

      private fun getFileName(uri: Uri): String {
        var fileName = ""
        val columns = arrayOf(OpenableColumns.DISPLAY_NAME)
        context.contentResolver.query(uri, columns, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          cursor.moveToFirst()
          fileName = cursor.getString(nameIndex)
        }
        return fileName
      }
    }


  private fun createDocumentReference(attachmentUri: Uri, mimeType: String): DocumentReference {
    val doc = DocumentReference().apply {
      id = UUID.randomUUID().toString()
      addExtension(EXTENSION_FILE_LOCATION, StringType(attachmentUri.toString()))
      addContent().apply {
        attachment = Attachment().apply { contentType = mimeType }
      }
      date = Date()
      docStatus = DocumentReference.ReferredDocumentStatus.FINAL
      status = Enumerations.DocumentReferenceStatus.CURRENT
    }
    return doc
  }

  private val IMAGE_CACHE_BASE_URI: String = "content://org.smartregister.opensrp.fileprovider/cache/"
  val EXTRA_MIME_TYPE_KEY = "mime_type"
  val EXTRA_SAVED_PHOTO_URI_KEY = "saved_photo_uri"

  fun matcher(questionnaireItem: Questionnaire.QuestionnaireItemComponent): Boolean {
    return questionnaireItem.type == Questionnaire.QuestionnaireItemType.ATTACHMENT
  }

}

private fun getMimeType(mimeType: String): String = mimeType.substringBefore("/")

private fun Context.readBytesFromUri(uri: Uri): ByteArray {
  return contentResolver.openInputStream(uri)?.use { it.buffered().readBytes() } ?: ByteArray(0)
}

private fun Context.getMimeTypeFromUri(uri: Uri): String {
  return contentResolver.getType(uri) ?: "*/*"
}


internal const val EXTENSION_MAX_SIZE = "http://hl7.org/fhir/StructureDefinition/maxSize"
internal const val EXTENSION_FILE_LOCATION = "http://hl7.org/fhir/StructureDefinition/file-location"

/** The default maximum size of an attachment is 1 Mebibytes. */
private val DEFAULT_SIZE = BigDecimal(1048576)

/** The maximum size of an attachment in Bytes. */
internal val Questionnaire.QuestionnaireItemComponent.maxSizeInBytes: BigDecimal?
  get() =
    (extension.firstOrNull { it.url == EXTENSION_MAX_SIZE }?.valueAsPrimitive as DecimalType?)
      ?.value

private val BYTES_PER_MIB = BigDecimal(1048576)

/** The maximum size of an attachment in Mebibytes. */
internal val Questionnaire.QuestionnaireItemComponent.maxSizeInMiBs: BigDecimal?
  get() = maxSizeInBytes?.div(BYTES_PER_MIB)

/** Returns true if given size is above maximum size allowed. */
private fun Questionnaire.QuestionnaireItemComponent.isGivenSizeOverLimit(
  size: BigDecimal,
): Boolean {
  return size > (maxSizeInBytes ?: DEFAULT_SIZE)
}

private val DocumentReference.url
  get() = "${BuildConfig.FHIR_BASE_URL}DocumentReference/${logicalId}/\$binary-access-read?path=DocumentReference.content.attachment"