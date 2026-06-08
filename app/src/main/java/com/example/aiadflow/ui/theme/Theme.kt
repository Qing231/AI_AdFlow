package com.example.aiadflow.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Material3 浅色主题配色，绑定到应用自定义颜色。 */
private val LightColorScheme = lightColorScheme(
    primary = AppColors.Primary,
    onPrimary = AppColors.OnPrimary,
    background = AppColors.PageBackground,
    surface = AppColors.Surface,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary
)

/** 应用统一圆角定义。 */
object AppRadius {
    /** 中等圆角，用于媒体区域等内层元素。 */
    val Medium = RoundedCornerShape(12.dp)
    /** 大圆角，用于广告卡片和搜索框。 */
    val Large = RoundedCornerShape(20.dp)
    /** 胶囊圆角，用于 tab、标签和操作按钮。 */
    val Full = RoundedCornerShape(999.dp)
}

/** 应用统一尺寸和间距定义。 */
object AppSpacing {
    /** 小间距，用于紧凑元素之间。 */
    val Small = 8.dp
    /** 标准内容内边距。 */
    val Medium = 16.dp
    /** 页面区块之间的垂直间距。 */
    val Section = 18.dp
    /** 页面左右安全边距。 */
    val PageHorizontal = 20.dp
    /** 顶部标题栏高度。 */
    val HeaderHeight = 72.dp
    /** 右上角圆形入口尺寸。 */
    val IconButton = 44.dp
    /** 频道 tab 高度。 */
    val TabHeight = 44.dp
    /** 频道 tab 选中态高度，用于切换时的轻微尺寸动效。 */
    val TabSelectedHeight = 48.dp
    /** 频道 tab 固定宽度，保证横向滚动时每个频道尺寸稳定。 */
    val TabWidth = 78.dp
    /** 频道 tab 未选中态描边宽度。 */
    val TabBorderWidth = 1.dp
    /** 搜索框高度。 */
    val SearchHeight = 56.dp
    /** 搜索框描边宽度。 */
    val SearchBorderWidth = 1.dp
    /** 搜索框内部分割线高度。 */
    val SearchDividerHeight = 24.dp
    /** 搜索框内部分割线宽度。 */
    val SearchDividerWidth = 1.dp
    /** 搜索框清除按钮尺寸。 */
    val SearchClearButton = 28.dp
    /** 大图和视频广告的媒体区域高度。 */
    val AdMediaHeight = 168.dp
    /** 视频广告封面区域高度。 */
    val VideoMediaHeight = 196.dp
    /** 大图广告的主视觉区域高度。 */
    val LargeImageMediaHeight = 220.dp
    /** 小图和图文广告的媒体区域高度。 */
    val CompactMediaHeight = 124.dp
    /** 图文广告的图片区域高度。 */
    val ImageTextMediaHeight = 144.dp
    /** 小图广告左侧缩略图宽度。 */
    val SmallImageMediaWidth = 128.dp
    /** 视频播放入口尺寸。 */
    val PlayButton = 56.dp
    /** 标签 chip 的垂直内边距。 */
    val TagVertical = 6.dp
    val TagBorderWidth = 1.dp
    val TagMaxWidth = 132.dp
    /** 操作按钮高度。 */
    val ActionHeight = 36.dp
    /** 空状态卡片高度。 */
    val EmptyHeight = 160.dp
}

/** 应用 Compose 主题入口，统一注入颜色和字体。 */
@Composable
fun AIAdFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
