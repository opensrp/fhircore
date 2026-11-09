/*
 * Copyright 2021-2024 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.smartregister.fhircore.quest.ui.questionnaire.attachment

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.fhir.datacapture.R
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
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderDelegate
import com.google.android.fhir.datacapture.views.factories.QuestionnaireItemAndroidViewHolderFactory
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Attachment
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemComponent
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.smartregister.fhircore.engine.R as EngineR
import org.smartregister.fhircore.engine.ui.base.AlertDialogue
import org.smartregister.fhircore.quest.R as QuestR
import org.smartregister.fhircore.quest.ui.questionnaire.signature.isSignatureItem
import timber.log.Timber

/** Every attachment item except a signature, which has its own widget. */
fun isAttachmentItem(item: QuestionnaireItemComponent): Boolean =
  item.type == Questionnaire.QuestionnaireItemType.ATTACHMENT && !isSignatureItem(item)

/**
 * The attachment widget, adapted from the Android FHIR SDK's AttachmentViewHolderFactory (Copyright
 * 2022-2025 Google LLC, Apache License 2.0), which is internal to the SDK. It keeps the SDK's
 * layout. The changes:
 * - A picture is shrunk with [shrinkPhoto] before the item's maxSize check, so a large phone photo
 *   is accepted and saved at [MAX_PHOTO_BYTES] or less.
 * - Any other file, such as a PDF, over [MAX_OTHER_FILE_BYTES] is refused.
 * - Files are read and shrunk off the main thread, behind a « Please wait » dialog that blocks the
 *   form until the answer is saved. The answer goes to the item the worker tapped. A file that
 *   can't be read shows an error under the item, where the SDK's widget crashes.
 * - Binding resets the buttons and the « Uploaded » label, so a reused view doesn't keep them from
 *   another item.
 *
 * Limit: the occurrences of a repeating group share one questionnaire item, so [isShowing] can't
 * tell them apart (the SDK's hasTheSameItem() is internal). The dialog stops the list scrolling
 * while a file is added, so a view isn't reused for another occurrence in that time.
 */
object AttachmentItemViewHolderFactory :
  QuestionnaireItemAndroidViewHolderFactory(R.layout.attachment_view) {
  override fun getQuestionnaireItemViewHolderDelegate() =
    object : QuestionnaireItemAndroidViewHolderDelegate {
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
      private lateinit var filePreview: ConstraintLayout
      private lateinit var fileIcon: ImageView
      private lateinit var fileTitle: TextView
      private lateinit var fileDeleteButton: Button
      private lateinit var context: AppCompatActivity

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
        filePreview = itemView.findViewById(R.id.file_preview)
        fileIcon = itemView.findViewById(R.id.file_icon)
        fileTitle = itemView.findViewById(R.id.file_title)
        fileDeleteButton = itemView.findViewById(R.id.file_delete)
        context = itemView.context.tryUnwrapContext()!!
      }

      override fun bind(questionnaireViewItem: QuestionnaireViewItem) {
        this.questionnaireViewItem = questionnaireViewItem
        header.bind(questionnaireViewItem, showRequiredOrOptionalText = true)
        val questionnaireItem = questionnaireViewItem.questionnaireItem
        displayOrClearInitialPreview()
        hideAllButtons()
        displayTakePhotoButton(questionnaireItem)
        displayUploadButton(questionnaireItem)
        takePhotoButton.setOnClickListener { view -> onTakePhotoClicked(view) }
        uploadPhotoButton.setOnClickListener { view -> onUploadClicked(view) }
        uploadAudioButton.setOnClickListener { view -> onUploadClicked(view) }
        uploadVideoButton.setOnClickListener { view -> onUploadClicked(view) }
        uploadDocumentButton.setOnClickListener { view -> onUploadClicked(view) }
        uploadFileButton.setOnClickListener { view -> onUploadClicked(view) }
        photoDeleteButton.setOnClickListener { view -> onDeleteClicked(view) }
        fileDeleteButton.setOnClickListener { view -> onDeleteClicked(view) }
        displayValidationResult(questionnaireViewItem.validationResult)
      }

      private fun displayValidationResult(validationResult: ValidationResult) {
        when (validationResult) {
          is NotValidated,
          Valid, -> errorTextView.visibility = View.GONE
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
          divider.visibility = View.GONE
          labelUploaded.visibility = View.GONE
          clearPhotoPreview()
          clearFilePreview()
          return
        }

        answer.valueAttachment?.let { attachment ->
          divider.visibility = View.VISIBLE
          labelUploaded.visibility = View.VISIBLE
          displayPreview(
            attachmentType = getMimeType(attachment.contentType),
            attachmentTitle = attachment.title,
            attachmentByteArray = attachment.data,
          )
        }
      }

      /** A reused view keeps the buttons its last item showed. */
      private fun hideAllButtons() {
        listOf(
            takePhotoButton,
            uploadPhotoButton,
            uploadAudioButton,
            uploadVideoButton,
            uploadDocumentButton,
            uploadFileButton,
          )
          .forEach { it.visibility = View.GONE }
      }

      private fun displayTakePhotoButton(questionnaireItem: QuestionnaireItemComponent) {
        if (questionnaireItem.hasMimeType(MimeType.IMAGE.value)) {
          takePhotoButton.visibility = View.VISIBLE
        }
      }

      private fun displayUploadButton(questionnaireItem: QuestionnaireItemComponent) {
        when {
          questionnaireItem.hasMimeTypeOnly(MimeType.AUDIO.value) -> {
            uploadAudioButton.visibility = View.VISIBLE
          }
          questionnaireItem.hasMimeTypeOnly(MimeType.DOCUMENT.value) -> {
            uploadDocumentButton.visibility = View.VISIBLE
          }
          questionnaireItem.hasMimeTypeOnly(MimeType.IMAGE.value) -> {
            uploadPhotoButton.visibility = View.VISIBLE
          }
          questionnaireItem.hasMimeTypeOnly(MimeType.VIDEO.value) -> {
            uploadVideoButton.visibility = View.VISIBLE
          }
          else -> {
            uploadFileButton.visibility = View.VISIBLE
          }
        }
      }

      private fun onTakePhotoClicked(view: View) {
        val viewItem = questionnaireViewItem
        val file = File.createTempFile("IMG_", ".jpeg", context.cacheDir)
        val attachmentUri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        context.supportFragmentManager.setFragmentResultListener(
          CameraLauncherFragment.CAMERA_RESULT_KEY,
          context,
        ) { _, result ->
          if (!result.getBoolean(CameraLauncherFragment.CAMERA_RESULT_KEY)) {
            file.delete()
            return@setFragmentResultListener
          }
          context.lifecycleScope.launch {
            try {
              addFile(view, viewItem, attachmentUri, file.name)
            } finally {
              file.delete()
            }
          }
        }

        CameraLauncherFragment()
          .apply { arguments = bundleOf(EXTRA_SAVED_PHOTO_URI_KEY to attachmentUri) }
          .show(
            context.supportFragmentManager,
            AttachmentItemViewHolderFactory.javaClass.simpleName,
          )
      }

      private fun onUploadClicked(view: View) {
        val viewItem = questionnaireViewItem
        context.supportFragmentManager.setFragmentResultListener(
          OpenDocumentLauncherFragment.OPEN_DOCUMENT_RESULT_KEY,
          context,
        ) { _, result ->
          val attachmentUri =
            (result.get(OpenDocumentLauncherFragment.OPEN_DOCUMENT_RESULT_KEY)
              ?: return@setFragmentResultListener)
              as Uri
          context.lifecycleScope.launch {
            addFile(view, viewItem, attachmentUri, context.getFileName(attachmentUri))
          }
        }

        OpenDocumentLauncherFragment()
          .apply {
            arguments =
              bundleOf(
                EXTRA_MIME_TYPE_KEY to viewItem.questionnaireItem.mimeTypes.toTypedArray(),
              )
          }
          .show(
            context.supportFragmentManager,
            AttachmentItemViewHolderFactory.javaClass.simpleName,
          )
      }

      /**
       * Checks the file the worker added to [viewItem]'s item, shrinks it if it is a picture, and
       * saves it as the item's answer.
       */
      private suspend fun addFile(
        view: View,
        viewItem: QuestionnaireViewItem,
        attachmentUri: Uri,
        attachmentTitle: String,
      ) {
        val questionnaireItem = viewItem.questionnaireItem
        val attachmentMimeTypeWithSubType = context.getMimeTypeFromUri(attachmentUri)
        if (attachmentMimeTypeWithSubType == null) {
          displayError(viewItem, context.getString(QuestR.string.attachment_file_unreadable))
          displaySnackbar(view, R.string.upload_failed)
          return
        }
        val attachmentMimeType = getMimeType(attachmentMimeTypeWithSubType)
        if (!questionnaireItem.hasMimeType(attachmentMimeType)) {
          displayError(
            viewItem,
            context.getString(R.string.mime_type_wrong_media_format_validation_error_msg),
          )
          displaySnackbar(view, R.string.upload_failed)
          return
        }

        // Until the answer is saved, a SUBMIT would miss this file, and a delete or another upload
        // would be undone by it
        val progress = AlertDialogue.showProgressAlert(context, EngineR.string.please_wait)
        try {
          saveFile(view, viewItem, attachmentUri, attachmentTitle, attachmentMimeTypeWithSubType)
        } finally {
          // The activity may be gone if the coroutine was cancelled
          runCatching { progress.dismiss() }
        }
      }

      private suspend fun saveFile(
        view: View,
        viewItem: QuestionnaireViewItem,
        attachmentUri: Uri,
        attachmentTitle: String,
        attachmentMimeTypeWithSubType: String,
      ) {
        val questionnaireItem = viewItem.questionnaireItem
        val attachmentMimeType = getMimeType(attachmentMimeTypeWithSubType)
        val isPhoto = attachmentMimeType == MimeType.IMAGE.value && questionnaireItem.acceptsJpeg()
        // A file over the limit is read only to one byte past it. For any file but a photo, the
        // size checks below then refuse it
        val readLimit = if (isPhoto) MAX_PHOTO_INPUT_BYTES else MAX_OTHER_FILE_BYTES
        val bytes =
          withContext(Dispatchers.IO) { context.readBytesFromUri(attachmentUri, readLimit) }
        if (bytes == null) {
          displayError(viewItem, context.getString(QuestR.string.attachment_file_unreadable))
          displaySnackbar(view, R.string.upload_failed)
          return
        }
        if (isPhoto && bytes.size > readLimit) {
          displayError(
            viewItem,
            context.getString(
              QuestR.string.attachment_file_too_large,
              Formatter.formatShortFileSize(context, readLimit.toLong()),
            ),
          )
          displaySnackbar(view, R.string.upload_failed)
          return
        }
        val file =
          withContext(Dispatchers.Default) {
            if (isPhoto) {
              shrinkPhoto(bytes, attachmentMimeTypeWithSubType)
            } else {
              SavedFile(bytes, attachmentMimeTypeWithSubType)
            }
          }

        val error =
          when {
            file == null -> context.getString(QuestR.string.attachment_photo_unreadable)
            questionnaireItem.isGivenSizeOverLimit(file.bytes.size) ->
              context.getString(
                if (isPhoto) {
                  R.string.max_size_image_above_limit_validation_error_msg
                } else {
                  R.string.max_size_file_above_limit_validation_error_msg
                },
                questionnaireItem.maxSizeInMiBs,
              )
            !isPhoto && file.bytes.size > MAX_OTHER_FILE_BYTES ->
              context.getString(
                QuestR.string.attachment_file_too_large,
                Formatter.formatShortFileSize(context, MAX_OTHER_FILE_BYTES.toLong()),
              )
            else -> null
          }
        if (error != null || file == null) {
          displayError(viewItem, error)
          displaySnackbar(view, R.string.upload_failed)
          return
        }

        val savedTitle =
          if (file.contentType == attachmentMimeTypeWithSubType) {
            attachmentTitle
          } else {
            attachmentTitle.substringBeforeLast('.') + ".jpg"
          }
        val answer =
          QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().apply {
            value =
              Attachment().apply {
                contentType = file.contentType
                data = file.bytes
                title = savedTitle
                creation = Date()
              }
          }
        viewItem.setAnswer(answer)

        if (isShowing(viewItem)) {
          divider.visibility = View.VISIBLE
          labelUploaded.visibility = View.VISIBLE
          displayPreview(
            attachmentType = getMimeType(file.contentType),
            attachmentTitle = savedTitle,
            attachmentByteArray = file.bytes,
          )
        }
        displaySnackbarOnUpload(view, attachmentMimeType)
      }

      /** True while this view still shows [viewItem]'s item, and not another one. */
      private fun isShowing(viewItem: QuestionnaireViewItem) =
        questionnaireViewItem.questionnaireItem === viewItem.questionnaireItem

      private fun displayPreview(
        attachmentType: String,
        attachmentTitle: String,
        attachmentByteArray: ByteArray? = null,
        attachmentUri: Uri? = null,
      ) {
        when (attachmentType) {
          MimeType.AUDIO.value -> {
            loadFilePreview(R.drawable.ic_audio_file, attachmentTitle)
            clearPhotoPreview()
          }
          MimeType.DOCUMENT.value -> {
            loadFilePreview(R.drawable.ic_document_file, attachmentTitle)
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
            loadFilePreview(R.drawable.ic_video_file, attachmentTitle)
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
            displaySnackbar(view, R.string.file_uploaded)
          }
          MimeType.IMAGE.value -> {
            displaySnackbar(view, R.string.image_uploaded)
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
            displaySnackbar(view, R.string.file_deleted)
          }
          MimeType.IMAGE.value -> {
            displaySnackbar(view, R.string.image_deleted)
          }
          MimeType.VIDEO.value -> {
            displaySnackbar(view, R.string.video_deleted)
          }
        }
      }

      /** Shows [message] under [viewItem]'s item, if this view still shows it. */
      private fun displayError(viewItem: QuestionnaireViewItem, message: String?) {
        if (message != null && isShowing(viewItem)) {
          displayValidationResult(Invalid(listOf(message)))
        }
      }
    }

  // The SDK's launcher fragments read their arguments under these keys
  private const val EXTRA_MIME_TYPE_KEY = "mime_type"
  private const val EXTRA_SAVED_PHOTO_URI_KEY = "saved_photo_uri"
}

private const val EXTENSION_MAX_SIZE = "http://hl7.org/fhir/StructureDefinition/maxSize"

/** The SDK's limit for an item with no maxSize: 1 MiB. */
private val DEFAULT_MAX_SIZE = BigDecimal(1_048_576)

private val BYTES_PER_MIB = BigDecimal(1_048_576)

private val QuestionnaireItemComponent.maxSizeInBytes: BigDecimal?
  get() =
    (extension.firstOrNull { it.url == EXTENSION_MAX_SIZE }?.valueAsPrimitive as DecimalType?)
      ?.value

private val QuestionnaireItemComponent.maxSizeInMiBs: BigDecimal?
  get() = maxSizeInBytes?.div(BYTES_PER_MIB)

private fun QuestionnaireItemComponent.isGivenSizeOverLimit(size: Int): Boolean =
  size.toBigDecimal() > (maxSizeInBytes ?: DEFAULT_MAX_SIZE)

/** True if the item takes JPEG pictures, the type a shrunk photo is saved as. */
private fun QuestionnaireItemComponent.acceptsJpeg(): Boolean =
  mimeTypes.any { it == "image/*" || it == JPEG_CONTENT_TYPE }

/** Returns the main MIME type of a MIME type string (e.g. image/png returns image). */
private fun getMimeType(mimeType: String): String = mimeType.substringBefore("/")

/**
 * The file's bytes, or null if it can't be read: gone, empty, or refused by its provider. Reads at
 * most [maxBytes] + 1 bytes, so a result longer than [maxBytes] means the file is over the limit.
 */
@VisibleForTesting
internal fun Context.readBytesFromUri(uri: Uri, maxBytes: Int): ByteArray? =
  readFromProvider { contentResolver.openInputStream(uri)?.use { it.readAtMost(maxBytes + 1) } }
    ?.takeIf { it.isNotEmpty() }

/** Reads until the end of the stream or [count] bytes, whichever comes first. */
private fun InputStream.readAtMost(count: Int): ByteArray {
  val output = ByteArrayOutputStream()
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  while (output.size() < count) {
    val read = read(buffer, 0, minOf(buffer.size, count - output.size()))
    if (read < 0) break
    output.write(buffer, 0, read)
  }
  return output.toByteArray()
}

/** The file's MIME type, `*` `/` `*` if its provider doesn't say, or null if the provider fails. */
@VisibleForTesting
internal fun Context.getMimeTypeFromUri(uri: Uri): String? = readFromProvider {
  contentResolver.getType(uri) ?: "*/*"
}

/**
 * The file's display name. If its provider fails or has no name, for example after the read
 * permission is revoked, the last segment of [uri], or an empty string. The read that follows then
 * fails and shows the error.
 */
@VisibleForTesting
internal fun Context.getFileName(uri: Uri): String =
  readFromProvider {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
      cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
  } ?: uri.lastPathSegment ?: ""

/**
 * Runs [block], a call to a file's content provider. Returns null instead of crashing when the
 * provider fails: the file is gone, the permission was revoked, or the provider refuses the call.
 * Cancellation still propagates, although CancellationException is an IllegalStateException.
 */
private inline fun <T> readFromProvider(block: () -> T): T? =
  try {
    block()
  } catch (exception: Exception) {
    if (exception is CancellationException || !exception.isProviderFailure()) throw exception
    Timber.w(exception, "Can't read the attachment")
    null
  }

private fun Exception.isProviderFailure() =
  this is IOException ||
    this is SecurityException ||
    this is IllegalArgumentException ||
    this is IllegalStateException ||
    this is UnsupportedOperationException
