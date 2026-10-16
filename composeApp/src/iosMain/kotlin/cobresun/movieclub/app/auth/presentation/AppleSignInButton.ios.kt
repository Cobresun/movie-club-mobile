@file:OptIn(ExperimentalForeignApi::class)

package cobresun.movieclub.app.auth.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitView
import cobresun.movieclub.app.auth.domain.AppleCredential
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationAppleIDButton
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonStyle
import platform.AuthenticationServices.ASAuthorizationAppleIDButtonTypeSignIn
import platform.AuthenticationServices.ASAuthorizationAppleIDCredential
import platform.AuthenticationServices.ASAuthorizationAppleIDProvider
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationScopeEmail
import platform.AuthenticationServices.ASAuthorizationScopeFullName
import platform.AuthenticationServices.ASPresentationAnchor
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSError
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIControlEventTouchUpInside
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import kotlin.random.Random

@Composable
actual fun AppleSignInButton(
    enabled: Boolean,
    onOutcome: (AppleSignInOutcome) -> Unit,
    modifier: Modifier,
) {
    val currentOnOutcome by rememberUpdatedState(onOutcome)
    val coordinator = remember { AppleSignInCoordinator { currentOnOutcome(it) } }
    DisposableEffect(coordinator) { onDispose { coordinator.dispose() } }

    UIKitView(
        factory = {
            ASAuthorizationAppleIDButton(
                authorizationButtonType = ASAuthorizationAppleIDButtonTypeSignIn,
                authorizationButtonStyle = ASAuthorizationAppleIDButtonStyle.ASAuthorizationAppleIDButtonStyleWhite,
            ).apply {
                cornerRadius = 12.0
                addTarget(coordinator, platform.objc.sel_registerName("start"), UIControlEventTouchUpInside)
            }
        },
        update = { button ->
            button.enabled = enabled
            button.alpha = if (enabled) 1.0 else 0.5
        },
        modifier = modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(12.dp)),
    )
}

/** Runs one `ASAuthorizationController` request at a time and reports the result. */
@OptIn(BetaInteropApi::class)
private class AppleSignInCoordinator(
    private val onOutcome: (AppleSignInOutcome) -> Unit,
) : NSObject(),
    ASAuthorizationControllerDelegateProtocol,
    ASAuthorizationControllerPresentationContextProvidingProtocol {

    private var controller: ASAuthorizationController? = null
    private var rawNonce: String? = null

    @ObjCAction
    fun start() {
        if (controller != null) return
        val nonce = randomNonce()
        rawNonce = nonce
        val request = ASAuthorizationAppleIDProvider().createRequest().apply {
            requestedScopes = listOf(ASAuthorizationScopeFullName, ASAuthorizationScopeEmail)
            // Apple embeds the hash; BetterAuth compares it against sha256(raw nonce).
            setNonce(sha256Hex(nonce))
        }
        controller = ASAuthorizationController(authorizationRequests = listOf(request)).also {
            it.delegate = this
            it.presentationContextProvider = this
            it.performRequests()
        }
    }

    fun dispose() {
        controller?.delegate = null
        controller = null
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization,
    ) {
        this.controller = null
        val credential = didCompleteWithAuthorization.credential as? ASAuthorizationAppleIDCredential
        val token = credential?.identityToken?.let {
            NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
        }
        if (credential == null || token == null) {
            onOutcome(AppleSignInOutcome.Failed("Apple did not return an identity token"))
            return
        }
        onOutcome(
            AppleSignInOutcome.Success(
                AppleCredential(
                    identityToken = token,
                    rawNonce = rawNonce,
                    givenName = credential.fullName?.givenName,
                    familyName = credential.fullName?.familyName,
                )
            )
        )
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError,
    ) {
        this.controller = null
        val cancelled = didCompleteWithError.domain == ASAuthorizationErrorDomain &&
            didCompleteWithError.code == ASAuthorizationErrorCanceled
        onOutcome(
            if (cancelled) AppleSignInOutcome.Cancelled
            else AppleSignInOutcome.Failed(didCompleteWithError.localizedDescription)
        )
    }

    override fun presentationAnchorForAuthorizationController(
        controller: ASAuthorizationController,
    ): ASPresentationAnchor {
        val scenes = UIApplication.sharedApplication.connectedScenes.filterIsInstance<UIWindowScene>()
        return scenes.firstNotNullOfOrNull { scene ->
            scene.windows.filterIsInstance<UIWindow>().firstOrNull { it.isKeyWindow() }
        } ?: UIWindow()
    }
}

private const val NONCE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._"

private fun randomNonce(length: Int = 32): String =
    buildString(length) { repeat(length) { append(NONCE_CHARS[Random.nextInt(NONCE_CHARS.length)]) } }

@OptIn(ExperimentalForeignApi::class)
private fun sha256Hex(input: String): String {
    val bytes = input.encodeToByteArray()
    return memScoped {
        val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), bytes.size.toUInt(), digest)
        }
        buildString {
            for (i in 0 until CC_SHA256_DIGEST_LENGTH) {
                append(digest[i].toString(16).padStart(2, '0'))
            }
        }
    }
}
