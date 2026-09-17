// SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
// SPDX-FileCopyrightText: 2026 ReVanced contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package app.reseam.patches.youtube.hide

import app.reseam.patch.ExtClass
import app.reseam.patch.FieldTarget
import app.reseam.patch.Type
import app.reseam.patch.after
import app.reseam.patch.before
import app.reseam.patch.classTarget
import app.reseam.patch.dex.AccessFlags
import app.reseam.patch.dex.Opcode
import app.reseam.patch.dex.opcode
import app.reseam.patch.dex.parameterTypes
import app.reseam.patch.dex.returnType
import app.reseam.patch.dex.typeRef
import app.reseam.patch.klass
import app.reseam.patch.method
import app.reseam.patch.methods
import app.reseam.patch.patch
import app.reseam.patch.point
import app.reseam.patch.points
import app.reseam.patch.replace
import app.reseam.patch.reserveLocal
import app.reseam.patch.settings.after
import app.reseam.patch.settings.before
import app.reseam.patch.settings.section
import app.reseam.patches.youtube.core.YOUTUBE
import app.reseam.patches.youtube.core.YouTubeSettings
import app.reseam.patches.youtube.core.YouTubeSettingsPages
import app.reseam.patches.youtube.core.youTubeSettings
import app.reseam.patches.youtube.internal.appHelper
import app.reseam.patches.youtube.internal.emptyComponentBuilder
import app.reseam.patches.youtube.internal.emptyComponentField
import app.reseam.patches.youtube.internal.engagementPanelHook
import app.reseam.patches.youtube.internal.lithoFilter
import app.reseam.patches.youtube.internal.registerLithoFilter

private const val VIEW_DESCRIPTOR = "Landroid/view/View;"
private const val TEXT_VIEW = "android.widget.TextView"
private const val FRAME_LAYOUT = "android.widget.FrameLayout"
private const val IMAGE_VIEW = "android.widget.ImageView"
private const val TYPED_VALUE = "Landroid/util/TypedValue;"
private const val DISPLAY_METRICS = "Landroid/util/DisplayMetrics;"
private const val COLLECTION = "java.util.Collection"
private const val SEARCH_SUGGESTION_ACCESSOR =
    "Lapp/reseam/youtube/hidelayout/LayoutComponentsFilter\$SearchSuggestionAccessor;"

val hideLayoutComponents = patch("Hide layout components") {
    description("Adds options to hide feed, player, description, comment and channel components.")
    compatibleWith(YOUTUBE)
    dependsOn(lithoFilter, engagementPanelHook, app.reseam.patches.youtube.internal.navigationBarHook)
    settings(
        youTubeSettings,
        section(
            YouTubeSettingsPages.Feed,
            "Feed",
            YouTubeSettings.hideAlbumCards,
            YouTubeSettings.hideArtistCards,
            YouTubeSettings.hideChipsShelf,
            YouTubeSettings.hideCompactBanner,
            YouTubeSettings.hideCommunityPosts,
            YouTubeSettings.hideCreatorStoreShelf,
            YouTubeSettings.hideCrowdfundingBox,
            YouTubeSettings.hideDoodles,
            YouTubeSettings.hideEmergencyBox,
            YouTubeSettings.hideExpandableCard,
            YouTubeSettings.hideFeedFlyoutMenu,
            YouTubeSettings.hideFeedFlyoutMenuFilterStrings,
            YouTubeSettings.hideFilterBarFeedInFeed,
            YouTubeSettings.hideFilterBarFeedInHistory,
            YouTubeSettings.hideFilterBarFeedInRelatedVideos,
            YouTubeSettings.hideFilterBarFeedInSearch,
            YouTubeSettings.hideFloatingMicrophoneButton,
            YouTubeSettings.hideForYouShelf,
            YouTubeSettings.hideHorizontalShelves,
            YouTubeSettings.hideImageShelf,
            YouTubeSettings.hideLatestPosts,
            YouTubeSettings.hideLatestVideosButton,
            YouTubeSettings.hideLiveChatReplayButton,
            YouTubeSettings.hideMedicalPanels,
            YouTubeSettings.hideMixPlaylists,
            YouTubeSettings.hideMoviesSection,
            YouTubeSettings.hidePlayables,
            YouTubeSettings.hideQuickActions,
            YouTubeSettings.hideRelatedVideos,
            YouTubeSettings.hideShowMoreButton,
            YouTubeSettings.hideSurveys,
            YouTubeSettings.hideSubscribedChannelsBar,
            YouTubeSettings.hideTicketShelf,
            YouTubeSettings.hideTimedReactions,
            YouTubeSettings.hideVideoRecommendationLabels,
            YouTubeSettings.hideViewCount,
            YouTubeSettings.hideVisualSpacer,
            YouTubeSettings.hideWebSearchResults,
            YouTubeSettings.hideYouMayLikeSection,
        ),
        section(
            YouTubeSettingsPages.Player,
            "Player",
            YouTubeSettings.hideChannelBar,
            YouTubeSettings.hideChannelWatermark,
            YouTubeSettings.hideInfoPanels,
            YouTubeSettings.hideJoinMembershipButton,
            YouTubeSettings.hideNotifyMeButton,
            YouTubeSettings.hideSubscribersCommunityGuidelines,
            YouTubeSettings.hideUploadTime,
            YouTubeSettings.hideVideoTitle,
        ),
        section(
            YouTubeSettingsPages.Comments,
            "Comments",
            YouTubeSettings.hideCommentsAiChatSummary,
            YouTubeSettings.hideCommentsAiSummary,
            YouTubeSettings.hideCommentsByMembersHeader,
            YouTubeSettings.hideCommentsChannelGuidelines,
            YouTubeSettings.hideCommentsCommunityGuidelines,
            YouTubeSettings.hideCommentsCreateAShortButton,
            YouTubeSettings.hideCommentsEmojiAndTimestampButtons,
            YouTubeSettings.hideCommentsPreviewComment,
            YouTubeSettings.hideCommentsSection,
            YouTubeSettings.hideCommentsSectionInHomeFeed,
            YouTubeSettings.hideCommentsThanksButton,
        ),
        section(
            YouTubeSettingsPages.Description,
            "Description",
            YouTubeSettings.hideAiGeneratedVideoSummarySection,
            YouTubeSettings.hideAskSection,
            YouTubeSettings.hideAttributesSection,
            YouTubeSettings.hideChaptersSection,
            YouTubeSettings.hideCourseProgressSection,
            YouTubeSettings.hideExploreCourseSection,
            YouTubeSettings.hideExplorePodcastSection,
            YouTubeSettings.hideExploreSection,
            YouTubeSettings.hideFeaturedLinksSection,
            YouTubeSettings.hideFeaturedPlacesSection,
            YouTubeSettings.hideFeaturedVideosSection,
            YouTubeSettings.hideGamingSection,
            YouTubeSettings.hideHowThisWasMadeSection,
            YouTubeSettings.hideHypePoints,
            YouTubeSettings.hideInfoCardsSection,
            YouTubeSettings.hideKeyConceptsSection,
            YouTubeSettings.hideMusicSection,
            YouTubeSettings.hideSubscribeButton,
            YouTubeSettings.hideTranscriptSection,
            YouTubeSettings.hideQuizzesSection,
        ),
        section(
            YouTubeSettingsPages.Channel,
            "Channel",
            YouTubeSettings.hideChannelTab,
            YouTubeSettings.hideChannelTabFilterStrings,
            YouTubeSettings.hideCommunityButton,
            YouTubeSettings.hideJoinButton,
            YouTubeSettings.hideLinksPreview,
            YouTubeSettings.hideMembersShelf,
            YouTubeSettings.hideStoreButton,
            YouTubeSettings.hideSubscribeButtonInChannelPage,
        ),
        section(
            YouTubeSettingsPages.Filters,
            "Keyword filter",
            YouTubeSettings.hideKeywordContentHome,
            YouTubeSettings.hideKeywordContentSubscriptions,
            YouTubeSettings.hideKeywordContentSearch,
            YouTubeSettings.hideKeywordContentPhrases,
        ),
        section(YouTubeSettingsPages.Filters, "Custom filter", YouTubeSettings.customFilter, YouTubeSettings.customFilterStrings),
    )

    execute {
        registerLithoFilter(LayoutComponentsFilter)
        registerLithoFilter(DescriptionComponentsFilter)
        registerLithoFilter(CommentsFilter)
        registerLithoFilter(KeywordContentFilter)

        val linkTextStart = requireNotNull(resources.id("id", "link_text_start")) {
            "id/link_text_start is missing"
        }.toLong()
        val expandButtonContainer = requireNotNull(resources.id("id", "expand_button_container")) {
            "id/expand_button_container is missing"
        }.toLong()
        val showMoreSetView = method("hideShowMoreButtonSetView") {
            returns(Type.Void)
            literals(linkTextStart, expandButtonContainer)
        }
        val showMoreClass = classTarget("showMoreClass") {
            requireNotNull(bytecode.findClass(showMoreSetView.owner)) { "The Show more class is missing" }
        }
        val textViewField = fieldAfterLiteral(showMoreSetView, linkTextStart, "Landroid/widget/TextView;")
        val buttonContainerField = fieldAfterLiteral(showMoreSetView, expandButtonContainer, VIEW_DESCRIPTOR)
        val showMoreMethod = method("hideShowMoreButton") {
            inClass(showMoreClass)
            returns(Type.Void)
            paramCount(2)
            calls { owner(VIEW_DESCRIPTOR); name("setContentDescription"); params(Type.CharSequence) }
        }
        val parentViewMethod = method("showMoreParentView") {
            inClass(showMoreClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.View)
        }
        showMoreMethod.after(YouTubeSettings.hideShowMoreButton) {
            call(
                LayoutComponentsFilter.hideShowMoreButton,
                thisObject.call(parentViewMethod).cast(Type.View),
                thisObject.field(buttonContainerField).cast(Type.View),
                thisObject.field(textViewField).cast(TEXT_VIEW),
            )
        }

        val parentContainer = requireNotNull(resources.id("id", "parent_container")) {
            "id/parent_container is missing"
        }.toLong()
        val subscribedChannels = method("subscribedChannelsBarConstructor") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(parentContainer)
            custom { classDef.instanceFields.any { it.fieldType == "Landroid/support/v7/widget/RecyclerView;" } }
        }
        subscribedChannels.points("subscribedChannelsBar") {
            invokeVirtual { name("findViewById"); params(Type.Int); returns(Type.View) }
            argument(1) { literal(parentContainer) }
        }.single().next { resultOf(Type.View) }
            .captureAs("bar", Type.View)
            .after(YouTubeSettings.hideSubscribedChannelsBar) {
                call(LayoutComponentsFilter.hideSubscribedChannelsBar, capture("bar"))
            }

        val parentViewWidth = requireNotNull(resources.id("dimen", "parent_view_width_in_wide_mode")) {
            "dimen/parent_view_width_in_wide_mode is missing"
        }.toLong()
        val subscribedChannelsClass = klass(subscribedChannels.owner)
        val subscribedChannelsLandscape = method("subscribedChannelsLandscape") {
            inClass(subscribedChannelsClass)
            params()
            returns(Type.Void)
            literals(parentViewWidth)
        }
        subscribedChannelsLandscape.points("subscribedChannelsLandscapeHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(parentViewWidth) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after(YouTubeSettings.hideSubscribedChannelsBar) {
                capture("height").assign(int(0))
            }

        parseElementFromBuffer.after(YouTubeSettings.hideMixPlaylists) {
            whenTrue(call(LayoutComponentsFilter.filterMixPlaylists, param(1), param(2))) {
                returnValue(call(emptyComponentBuilder, param(0)).field(emptyComponentField))
            }
        }

        val playerOverlayClass = classTarget("playerOverlayClass") {
            requireNotNull(bytecode.findClass(playerOverlayMethod.owner)) { "The player overlay class is missing" }
        }
        val showWatermarkMethod = method("showWatermarkMethod") {
            inClass(playerOverlayClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params(Type.Context, Type.View)
            returns(Type.Void)
            calls { name("setImageBitmap") }
        }
        showWatermarkMethod.point("watermarkEnabled") {
            opcode(Opcode.IGET_BOOLEAN)
        }.captureAs("watermark", Type.Boolean).after(YouTubeSettings.hideChannelWatermark) {
            capture("watermark").assign(bool(false))
        }

        val crowdfundingBox = requireNotNull(resources.id("layout", "donation_companion")) {
            "layout/donation_companion is missing"
        }.toLong()
        val crowdfunding = method("crowdfundingBox") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(crowdfundingBox)
        }
        crowdfunding.points("crowdfundingView") {
            invokeVirtual { name("inflate"); returns(Type.View) }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after(YouTubeSettings.hideCrowdfundingBox) {
                call(LayoutComponentsFilter.hideCrowdfundingBox, capture("view"))
            }

        val albumCard = requireNotNull(resources.id("layout", "album_card")) {
            "layout/album_card is missing"
        }.toLong()
        val albumCards = method("albumCards") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(albumCard)
        }
        albumCards.points("albumCardView") {
            invokeVirtual { name("inflate"); returns(Type.View) }
            argument(1) { literal(albumCard) }
        }.single().next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after(YouTubeSettings.hideAlbumCards) {
                call(LayoutComponentsFilter.hideAlbumCard, capture("view"))
            }

        val fab = requireNotNull(resources.id("id", "fab")) { "id/fab is missing" }.toLong()
        val floatingMicrophone = method("floatingMicrophone") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Void)
            literals(fab)
        }
        floatingMicrophone.point("floatingMicrophoneResource") { literal(fab) }
            .next {
                opcode(Opcode.CHECK_CAST)
                where { typeRef?.endsWith("/FloatingActionButton;") == true }
            }
            .next { opcode(Opcode.IGET_BOOLEAN) }
            .captureAs("visible", Type.Boolean)
            .after(YouTubeSettings.hideFloatingMicrophoneButton) {
            capture("visible").assign(bool(true))
        }

        val contentPill = requireNotNull(resources.id("layout", "content_pill")) {
            "layout/content_pill is missing"
        }.toLong()
        val bar = requireNotNull(resources.id("layout", "bar")) { "layout/bar is missing" }.toLong()
        listOf(
            method("latestVideosContentPill") {
                flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
                paramCount(2)
                returns(Type.Void)
                literals(contentPill)
                calls { name("inflate"); paramCount(3); returns(Type.View) }
            },
            method("latestVideosBar") {
                flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
                paramCount(2)
                returns(Type.Void)
                literals(bar)
                calls { name("inflate"); paramCount(3); returns(Type.View) }
            },
        ).forEach { target ->
            target.point("latestVideosButton") {
                invokeVirtual {
                    name("inflate")
                    paramCount(3)
                    returns(Type.View)
                }
            }
                .next { resultOf(Type.View) }
                .captureAs("view", Type.View)
                .after(YouTubeSettings.hideLatestVideosButton) {
                    call(LayoutComponentsFilter.hideLatestVideosButton, capture("view"))
                }
        }

        val youtubeLogo = requireNotNull(resources.id("id", "youtube_logo")) {
            "id/youtube_logo is missing"
        }.toLong()
        val yoodles = method("yoodlesImageView") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            paramCount(2)
            returns(Type.View)
            literals(youtubeLogo)
        }
        val setImageDrawable: app.reseam.patch.PointMatch.() -> Unit = {
            invokeVirtual {
                owner("Landroid/widget/ImageView;")
                name("setImageDrawable")
                params("android.graphics.drawable.Drawable")
                returns(Type.Void)
            }
        }
        yoodles.point("doodleDrawable", setImageDrawable)
            .captureArgumentAs("image", 0, IMAGE_VIEW)
            .captureArgumentAs("drawable", 1, "android.graphics.drawable.Drawable")
            .before(YouTubeSettings.hideDoodles) { capture("drawable").assign(nullObject) }

        val subtitle = hideViewCountMethod
        val subtitleDimension = subtitle.reserveLocal("subtitleDimension", Type.Float)
        subtitle.point("subtitleDimensionSource") { string("Has attachmentRuns but drawableRequester is missing.") }
            .next { invokeStatic { owner(TYPED_VALUE); name("applyDimension"); params(Type.Int, Type.Float, DISPLAY_METRICS); returns(Type.Float) } }
            .captureArgumentAs("dimension", 1, Type.Float)
            .before { local(subtitleDimension).assign(capture("dimension")) }
        subtitle.after {
            capture("result").assign(call(LayoutComponentsFilter.modifyFeedSubtitleSpan,
                capture("result"), local(subtitleDimension)))
        }

        val filterBarHeight = requireNotNull(resources.id("dimen", "filter_bar_height")) {
            "dimen/filter_bar_height is missing"
        }.toLong()
        val filterBar = method("filterBarHeight") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(filterBarHeight)
            hasParam("android.widget.LinearLayout")
        }
        filterBar.points("feedFilterBarHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(filterBarHeight) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after(YouTubeSettings.hideFilterBarFeedInFeed) {
                capture("height").assign(int(0))
            }

        val barContainerHeight = requireNotNull(resources.id("dimen", "bar_container_height")) {
            "dimen/bar_container_height is missing"
        }.toLong()
        val searchChipBar = method("searchChipBar") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(barContainerHeight)
            hasParam("android.widget.LinearLayout")
        }
        searchChipBar.points("searchFilterBarHeight") {
            invokeVirtual { name("getDimensionPixelSize"); params(Type.Int); returns(Type.Int) }
            argument(1) { literal(barContainerHeight) }
        }.single()
            .next { resultOf(Type.Int) }
            .captureAs("height", Type.Int)
            .after(YouTubeSettings.hideFilterBarFeedInSearch) {
                capture("height").assign(int(0))
            }

        val relatedChipMargin = requireNotNull(resources.id("layout", "related_chip_cloud_reduced_margins")) {
            "layout/related_chip_cloud_reduced_margins is missing"
        }.toLong()
        val relatedChipCloud = method("relatedChipCloud") {
            flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
            literals(relatedChipMargin)
        }
        relatedChipCloud.points("relatedFilterBar") {
            invokeVirtual { name("inflate"); returns(Type.View) }
            argument(1) { literal(relatedChipMargin) }
        }.single()
            .next { resultOf(Type.View) }
            .captureAs("view", Type.View)
            .after(YouTubeSettings.hideFilterBarFeedInRelatedVideos) {
                call(LayoutComponentsFilter.hideInRelatedVideos, capture("view"))
            }

        val suggestionDivider = requireNotNull(resources.id("dimen", "suggestion_category_divider_height")) {
            "dimen/suggestion_category_divider_height is missing"
        }.toLong()
        val searchSuggestions = method("searchBoxTypingString") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            paramCount(1)
            returns(Type.Void)
            literals(suggestionDivider)
            opcode(Opcode.IGET_OBJECT)
            calls { name("isEmpty"); params(); returns(Type.Boolean) }
        }
        val suggestionClass = classTarget("searchSuggestionClass") {
            requireNotNull(bytecode.findClass(searchSuggestionConstructor.owner)) {
                "The search suggestion class is missing"
            }
        }
        val suggestionEndpointMethod = method("searchSuggestionEndpoint") {
            inClass(suggestionClass)
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
            params()
            returns(Type.Boolean)
            calls { owner("Landroid/text/TextUtils;"); name("isEmpty") }
            opcode(Opcode.IGET_OBJECT)
        }
        val suggestionEndpointField = suggestionEndpointMethod
            .point("searchSuggestionEndpointField") {
                opcode(Opcode.IGET_OBJECT)
                field {
                    owner(suggestionEndpointMethod.owner)
                    type(Type.String)
                }
            }
            .field()
        val suggestionFields = searchSuggestionFields(searchSuggestions)
        suggestionClass.classDef.addInterface(SEARCH_SUGGESTION_ACCESSOR)
        val searchHistoryAccessor = appHelper(suggestionClass.classDef, "patch_isSearchHistory", "()Z")
        searchHistoryAccessor.replace {
            returnValue(
                call(
                    LayoutComponentsFilter.isSearchHistory,
                    thisObject,
                    thisObject.field(suggestionEndpointField),
                ),
            )
        }
        val stateType = searchSuggestions.parameterTypes.single()
        val helper = appHelper(searchSuggestions.method.classDef, "patch_setSearchSuggestions", "($stateType)V",
            AccessFlags.PRIVATE or AccessFlags.FINAL)
        helper.replace {
            whenTrue(call(LayoutComponentsFilter.hideYouMayLikeSection, param(0).field(suggestionFields.typedString))) {
                call(LayoutComponentsFilter.filterSearchSuggestions, param(0).field(suggestionFields.collection))
            }
            returnVoid()
        }
        searchSuggestions.before { thisObject.call(helper, param(0)) }

        val bottomSheetMenuItems = methods("bottomSheetMenuItems") {
            paramCount(1)
            strings("Text missing for BottomSheetMenuItem.")
            callsMethod { returnType == Type.CharSequence && parameterTypes.size == 1 && parameterTypes[0].startsWith("L") }
            custom { returnType.startsWith("L") }
        }
        bottomSheetMenuItems.forEach {
            point("bottomSheetMenuText") {
                invokeStatic { returns(Type.CharSequence); paramCount(1) }
            }.next { resultOf(Type.CharSequence) }
                .captureAs("text", Type.CharSequence)
                .after(YouTubeSettings.hideFeedFlyoutMenu) {
                    capture("text").assign(call(LayoutComponentsFilter.hideFlyoutMenu, capture("text")))
                }
        }

        val posterArtWidthDefault = requireNotNull(resources.id("dimen", "poster_art_width_default")) {
            "dimen/poster_art_width_default is missing"
        }.toLong()
        val contextualMenuItem = method("contextualMenuItem") {
            flags(AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.SYNTHETIC)
            returns(Type.Void)
            paramCount(2)
            literals(posterArtWidthDefault)
            calls { owner("Landroid/widget/TextView;"); name("setText") }
        }
        contextualMenuItem.point("contextualMenuText") {
            checkCast("Landroid/widget/TextView;")
            then(within = 5) {
                invokeVirtual { owner("Landroid/widget/TextView;"); name("setText"); params(Type.CharSequence) }
            }
        }
            .captureArgumentAs("textView", 0, TEXT_VIEW)
            .captureArgumentAs("text", 1, Type.CharSequence)
            .after(YouTubeSettings.hideFeedFlyoutMenu) {
                call(LayoutComponentsFilter.hideFlyoutMenuForTablet, capture("textView"), capture("text"))
            }

        val tabTitle = channelTabRenderer.point("channelTabTitle") {
            invokeVirtual { params(Type.String, Type.Boolean); returns(Type.CharSequence) }
        }.writer(1).field()
        val tabModel = channelTabRenderer.point("channelTabModel") {
            opcode(Opcode.IGET_OBJECT)
            field { type(tabTitle.owner) }
        }.field()
        channelTabRenderer.points("channelTabViews") {
            invoke { returns(Type.View); hasParam(Type.CharSequence) }
        }.forEach {
            next { resultOf(Type.View) }.captureAs("tab", Type.View)
                .after(YouTubeSettings.hideChannelTab) {
                    call(LayoutComponentsFilter.hideChannelTabView, capture("tab"), param(0).field(tabModel).field(tabTitle))
                }
        }
    }
}

object LayoutComponentsFilter : ExtClass("app.reseam.youtube.hidelayout.LayoutComponentsFilter") {
    val filterMixPlaylists = static("filterMixPlaylists", Type.Object, "[B", returns = Type.Boolean)
    val hideShowMoreButton = static("hideShowMoreButton", Type.View, Type.View, TEXT_VIEW)
    val hideSubscribedChannelsBar = static("hideSubscribedChannelsBar", Type.View)
    val hideCrowdfundingBox = static("hideCrowdfundingBox", Type.View)
    val hideAlbumCard = static("hideAlbumCard", Type.View)
    val hideLatestVideosButton = static("hideLatestVideosButton", Type.View)
    val modifyFeedSubtitleSpan = static("modifyFeedSubtitleSpan", Type.CharSequence, Type.Float, returns = Type.CharSequence)
    val hideInRelatedVideos = static("hideInRelatedVideos", Type.View)
    val hideFlyoutMenu = static("hideFlyoutMenu", Type.CharSequence, returns = Type.CharSequence)
    val hideFlyoutMenuForTablet = static("hideFlyoutMenu", TEXT_VIEW, Type.CharSequence)
    val hideChannelTabView = static("hideChannelTabView", Type.View, Type.String)
    val hideYouMayLikeSection = static("hideYouMayLikeSection", Type.String, returns = Type.Boolean)
    val isSearchHistory = static("isSearchHistory", Type.Object, Type.String, returns = Type.Boolean)
    val filterSearchSuggestions = static("filterSearchSuggestions", COLLECTION)
}

object DescriptionComponentsFilter : ExtClass("app.reseam.youtube.hidelayout.DescriptionComponentsFilter")
object CommentsFilter : ExtClass("app.reseam.youtube.hidelayout.CommentsFilter")
object KeywordContentFilter : ExtClass("app.reseam.youtube.hidelayout.KeywordContentFilter")

private val parseElementFromBuffer = method("parseElementFromBuffer") {
    paramCount(5)
    param(2, "[B")
    strings("Failed to parse Element.")
}

private val playerOverlayMethod = method("playerOverlayMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.FINAL)
    returns(Type.String)
    strings("player_overlay_in_video_programming")
}

private val hideViewCountMethod = method("hideViewCountMethod") {
    flags(AccessFlags.PUBLIC or AccessFlags.STATIC)
    returns(Type.CharSequence)
    strings("Has attachmentRuns but drawableRequester is missing.")
    hasParam(Type.Boolean)
    calls {
        owner("Landroid/util/TypedValue;")
        name("applyDimension")
        params(Type.Int, Type.Float, DISPLAY_METRICS)
        returns(Type.Float)
    }
}

private val channelTabRenderer = method("channelTabRenderer") {
    strings("Unsupported tabbed view controller insertion.")
    returns(Type.Void)
}

private val searchSuggestionConstructor = method("searchSuggestionConstructor") {
    flags(AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR)
    returns(Type.Void)
    strings("… ")
    hasParam(Type.List)
}

private fun fieldAfterLiteral(target: app.reseam.patch.MethodTarget, literal: Long, type: String): FieldTarget =
    target.point("fieldAfterResource_$literal") { literal(literal) }
        .next {
            opcode(Opcode.IPUT_OBJECT)
            field {
                owner(target.owner)
                type(type)
            }
        }
        .field()

private data class SearchSuggestionFields(val collection: FieldTarget, val typedString: FieldTarget)

private fun searchSuggestionFields(target: app.reseam.patch.MethodTarget): SearchSuggestionFields =
    SearchSuggestionFields(
        collection = target.point("searchSuggestionCollectionField") {
            opcode(Opcode.IGET_OBJECT)
            field { type(COLLECTION) }
        }.field(),
        typedString = target.point("searchSuggestionTypedStringField") {
            opcode(Opcode.IGET_OBJECT)
            field { type(Type.String) }
        }.field(),
    )
