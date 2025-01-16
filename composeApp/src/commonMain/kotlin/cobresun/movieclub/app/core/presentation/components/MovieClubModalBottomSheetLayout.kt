package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun MovieClubModalBottomSheetLayout(
    sheetContent: @Composable () -> Unit,
    sheetState: ModalBottomSheetState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetContent = { sheetContent() },
        sheetState = sheetState,
        sheetShape = MaterialTheme.shapes.large.copy(
            topStart = CornerSize(16.dp),
            topEnd = CornerSize(16.dp)
        )
    ) {
        content()
    }
}
