package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.assist.AssistContent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountByID;
import org.joinmastodon.android.api.requests.accounts.GetAccountFamiliarFollowers;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.accounts.GetOwnAccount;
import org.joinmastodon.android.api.requests.accounts.SetAccountFollowed;
import org.joinmastodon.android.api.requests.accounts.UpdateAccountCredentials;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.account_list.FamiliarFollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowerListFragment;
import org.joinmastodon.android.fragments.account_list.FollowingListFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.FamiliarFollowers;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.photoviewer.AvatarCropper;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.sheets.DecentralizationExplainerSheet;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.text.ImageSpanThatDoesNotBreakShitForNoGoodReason;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.CustomDrawingOrderLinearLayout;
import org.joinmastodon.android.ui.views.NestedRecyclerScrollView;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.joinmastodon.android.utils.ElevationOnScrollListener;
import org.parceler.Parcels;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class ProfileFragment extends LoaderFragment implements ScrollableToTop, AssistContentProviderFragment{
	private static final int AVATAR_RESULT=722;
	private static final int COVER_RESULT=343;

	private ImageView avatar;
	private CoverImageView cover;
	private View avatarBorder;
	private TextView name, username, usernameDomain, bio, followersCount, followersLabel, followingCount, followingLabel;
	private ProgressBarButton actionButton;
	private ViewPager2 pager;
	private NestedRecyclerScrollView scrollView;
	private ProfileFeaturedFragment featuredFragment;
	private AccountTimelineFragment timelineFragment;
	private ProfileAboutFragment aboutFragment;
	private SavedPostsTimelineFragment savedFragment;
	private TabLayout tabbar;
	private SwipeRefreshLayout refreshLayout;
	private View followersBtn, followingBtn;
	private EditText nameEdit, bioEdit;
	private ProgressBar actionProgress;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private TextView followsYouView;
	private LinearLayout countersLayout;
	private View nameEditWrap, bioEditWrap;
	private View tabsDivider;
	private View actionButtonWrap;
	private CustomDrawingOrderLinearLayout scrollableContent;
	private ImageButton qrCodeButton;
	private ProgressBar innerProgress;
	private View actions;
	private View familiarFollowersRow;
	private ImageView[] familiarFollowersAvatars;
	private TextView familiarFollowersLabel;

	private Account account;
	private String accountID;
	private Relationship relationship;
	private boolean isOwnProfile;
	private ArrayList<AccountField> fields=new ArrayList<>();
	private List<Account> familiarFollowers=List.of();

	private boolean isInEditMode, editDirty;
	private Uri editNewAvatar, editNewCover;
	private String profileAccountID;
	private boolean refreshing;
	private View fab;
	private WindowInsets childInsets;
	private PhotoViewer currentPhotoViewer;
	private boolean editModeLoading;
	private ElevationOnScrollListener onScrollListener;
	private Drawable tabsColorBackground;
	private boolean tabBarIsAtTop;
	private Animator tabBarColorAnim;
	private MenuItem editSaveMenuItem;
	private boolean savingEdits;
	private Runnable editModeBackCallback=this::onEditModeBackCallback;
	private HashSet<APIRequest<?>> relationshipRequests=new HashSet<>();

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		if(getArguments().containsKey("profileAccount")){
			account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
			profileAccountID=account.id;
			isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
			loaded=true;
			if(!isOwnProfile)
				loadRelationship();
		}else{
			profileAccountID=getArguments().getString("profileAccountID");
			if(!getArguments().getBoolean("noAutoLoad", false))
				loadData();
		}
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		for(APIRequest<?> req:relationshipRequests)
			req.cancel();
		relationshipRequests.clear();
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_profile, container, false);

		avatar=content.findViewById(R.id.avatar);
		cover=content.findViewById(R.id.cover);
		avatarBorder=content.findViewById(R.id.avatar_border);
		name=content.findViewById(R.id.name);
		username=content.findViewById(R.id.username);
		usernameDomain=content.findViewById(R.id.username_domain);
		bio=content.findViewById(R.id.bio);
		followersCount=content.findViewById(R.id.followers_count);
		followersLabel=content.findViewById(R.id.followers_label);
		followersBtn=content.findViewById(R.id.followers_btn);
		followingCount=content.findViewById(R.id.following_count);
		followingLabel=content.findViewById(R.id.following_label);
		followingBtn=content.findViewById(R.id.following_btn);
		actionButton=content.findViewById(R.id.profile_action_btn);
		pager=content.findViewById(R.id.pager);
		scrollView=content.findViewById(R.id.scroller);
		tabbar=content.findViewById(R.id.tabbar);
		refreshLayout=content.findViewById(R.id.refresh_layout);
		nameEdit=content.findViewById(R.id.name_edit);
		bioEdit=content.findViewById(R.id.bio_edit);
		nameEditWrap=content.findViewById(R.id.name_edit_wrap);
		bioEditWrap=content.findViewById(R.id.bio_edit_wrap);
		actionProgress=content.findViewById(R.id.action_progress);
		fab=content.findViewById(R.id.fab);
		followsYouView=content.findViewById(R.id.follows_you);
		countersLayout=content.findViewById(R.id.profile_counters);
		tabsDivider=content.findViewById(R.id.tabs_divider);
		actionButtonWrap=content.findViewById(R.id.profile_action_btn_wrap);
		scrollableContent=content.findViewById(R.id.scrollable_content);
		qrCodeButton=content.findViewById(R.id.qr_code);
		innerProgress=content.findViewById(R.id.profile_progress);
		actions=content.findViewById(R.id.profile_actions);
		familiarFollowersRow=content.findViewById(R.id.familiar_followers);
		familiarFollowersAvatars=new ImageView[]{
				content.findViewById(R.id.familiar_followers_ava1),
				content.findViewById(R.id.familiar_followers_ava2),
				content.findViewById(R.id.familiar_followers_ava3),
		};
		familiarFollowersLabel=content.findViewById(R.id.familiar_followers_label);

		avatar.setOutlineProvider(OutlineProviders.roundedRect(24));
		avatar.setClipToOutline(true);

		FrameLayout sizeWrapper=new FrameLayout(getActivity()){
			@Override
			protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
				pager.getLayoutParams().height=MeasureSpec.getSize(heightMeasureSpec)-getPaddingTop()-getPaddingBottom()-V.dp(48);
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		};

		tabViews=new FrameLayout[4];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			tabView.setId(switch(i){
				case 0 -> R.id.profile_featured;
				case 1 -> R.id.profile_timeline;
				case 2 -> R.id.profile_about;
				case 3 -> R.id.profile_saved;
				default -> throw new IllegalStateException("Unexpected value: "+i);
			});
			tabView.setVisibility(View.GONE);
			sizeWrapper.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		pager.setOffscreenPageLimit(10);
		pager.setAdapter(new ProfilePagerAdapter());
		pager.getLayoutParams().height=getResources().getDisplayMetrics().heightPixels;

		scrollView.setScrollableChildSupplier(this::getScrollableRecyclerView);

		sizeWrapper.addView(content);

		tabbar.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorM3OnSurfaceVariant), UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary));
		tabbar.setTabTextSize(V.dp(14));
		tabLayoutMediator=new TabLayoutMediator(tabbar, pager, (tab, position)->{
			tab.setText(switch(position){
				case 0 -> R.string.profile_featured;
				case 1 -> R.string.profile_timeline;
				case 2 -> R.string.profile_about;
				case 3 -> R.string.profile_saved_posts;
				default -> throw new IllegalStateException();
			});
			tab.view.textView.setSingleLine();
			tab.view.textView.setEllipsize(TextUtils.TruncateAt.END);
		});
		tabbar.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				if(getFragmentForPage(tab.getPosition()) instanceof ScrollableToTop stt)
					stt.scrollToTop();
			}
		});

		cover.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setEmpty();
			}
		});

		actionButton.setOnClickListener(this::onActionButtonClick);
		avatar.setOnClickListener(this::onAvatarClick);
		cover.setOnClickListener(this::onCoverClick);
		refreshLayout.setOnRefreshListener(this);
		fab.setOnClickListener(this::onFabClick);
		familiarFollowersRow.setOnClickListener(this::onFamiliarFollowersClick);

		if(savedInstanceState!=null){
			featuredFragment=(ProfileFeaturedFragment) getChildFragmentManager().getFragment(savedInstanceState, "featured");
			timelineFragment=(AccountTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "timeline");
			aboutFragment=(ProfileAboutFragment) getChildFragmentManager().getFragment(savedInstanceState, "about");
			savedFragment=(SavedPostsTimelineFragment) getChildFragmentManager().getFragment(savedInstanceState, "saved");
		}

		if(loaded){
			bindHeaderView();
			dataLoaded();
			tabLayoutMediator.attach();
		}else{
			fab.setVisibility(View.GONE);
		}

		followersBtn.setOnClickListener(this::onFollowersOrFollowingClick);
		followingBtn.setOnClickListener(this::onFollowersOrFollowingClick);

		username.setOnLongClickListener(v->{
			if(account==null)
				return true;
			String username=account.acct;
			if(!username.contains("@")){
				username+="@"+AccountSessionManager.getInstance().getAccount(accountID).domain;
			}
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, "@"+username));
			UiUtils.maybeShowTextCopiedToast(getActivity());
			return true;
		});

		scrollableContent.setDrawingOrderCallback((count, pos)->{
			// The header is the first child, draw it last to overlap everything for the photo viewer transition to look nice
			if(pos==count-1)
				return 0;
			// Offset the order of other child views to compensate
			return pos+1;
		});

		int colorBackground=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Background);
		int colorPrimary=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Primary);
		refreshLayout.setProgressBackgroundColorSchemeColor(UiUtils.alphaBlendColors(colorBackground, colorPrimary, 0.11f));
		refreshLayout.setColorSchemeColors(colorPrimary);

		nameEdit.addTextChangedListener(new SimpleTextWatcher(e->editDirty=true));
		bioEdit.addTextChangedListener(new SimpleTextWatcher(e->editDirty=true));

		usernameDomain.setOnClickListener(v->{
			if(account==null)
				return;
			new DecentralizationExplainerSheet(getActivity(), accountID, account).show();
		});
		qrCodeButton.setOnClickListener(v->{
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			ProfileQrCodeFragment qf=new ProfileQrCodeFragment();
			qf.setArguments(args);
			qf.show(getChildFragmentManager(), "qrDialog");
		});
		familiarFollowersRow.setVisibility(View.GONE);

		return sizeWrapper;
	}

	@Override
	protected void doLoadData(){
		currentRequest=new GetAccountByID(profileAccountID)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Account result){
						account=result;
						isOwnProfile=AccountSessionManager.getInstance().isSelf(accountID, account);
						bindHeaderView();
						dataLoaded();
						if(!tabLayoutMediator.isAttached())
							tabLayoutMediator.attach();
						if(!isOwnProfile)
							loadRelationship();
						else
							AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						if(refreshing){
							refreshing=false;
							refreshLayout.setRefreshing(false);
							if(timelineFragment.loaded)
								timelineFragment.onRefresh();
							if(featuredFragment.loaded)
								featuredFragment.onRefresh();
							if(savedFragment!=null && savedFragment.loaded)
								savedFragment.onRefresh();
						}
						V.setVisibilityAnimated(fab, View.VISIBLE);
					}
				})
				.exec(accountID);
	}

	@Override
	public void onRefresh(){
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(account));
		args.putBoolean("__is_tab", true);
		args.putBoolean("noAutoLoad", true);
		if(featuredFragment==null){
			featuredFragment=new ProfileFeaturedFragment();
			featuredFragment.setArguments(args);
		}
		if(timelineFragment==null){
			timelineFragment=AccountTimelineFragment.newInstance(accountID, account, true);
		}
		if(aboutFragment==null){
			aboutFragment=new ProfileAboutFragment();
			aboutFragment.setFields(fields);
		}
		if(savedFragment==null && isOwnProfile){
			savedFragment=SavedPostsTimelineFragment.newInstance(accountID, account, false);
		}
		if(!refreshing){
			pager.getAdapter().notifyDataSetChanged();
			pager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
				@Override
				public boolean onPreDraw(){
					pager.getViewTreeObserver().removeOnPreDrawListener(this);
					pager.setCurrentItem(1, false);
					tabbar.selectTab(tabbar.getTabAt(1));
					return true;
				}
			});
		}
		super.dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		// To avoid the callback triggering on first layout with position=0 before anything is instantiated
		pager.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				pager.getViewTreeObserver().removeOnPreDrawListener(this);
				pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
					@Override
					public void onPageSelected(int position){
						Fragment _page=getFragmentForPage(position);
						if(_page instanceof BaseRecyclerFragment<?> page && page.isAdded()){
							if(!page.loaded && !page.isDataLoading())
								page.loadData();
						}
					}

					@Override
					public void onPageScrollStateChanged(int state){
						if(isInEditMode)
							return;
						refreshLayout.setEnabled(state!=ViewPager2.SCROLL_STATE_DRAGGING);
					}
				});
				return true;
			}
		});

		tabsColorBackground=((LayerDrawable)tabbar.getBackground()).findDrawableByLayerId(R.id.color_overlay);

		onScrollListener=new ElevationOnScrollListener((FragmentRootLinearLayout) view, getToolbar());
		scrollView.setOnScrollChangeListener(this::onScrollChanged);
		scrollView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
			@Override
			public boolean onPreDraw(){
				scrollView.getViewTreeObserver().removeOnPreDrawListener(this);

				tabBarIsAtTop=!scrollView.canScrollVertically(1) && scrollView.getHeight()>0;
				tabsColorBackground.setAlpha(tabBarIsAtTop ? 20 : 0);
				tabbar.setTranslationZ(tabBarIsAtTop ? V.dp(3) : 0);
				tabsDivider.setAlpha(tabBarIsAtTop ? 0 : 1);

				return true;
			}
		});
		if(!loaded)
			bindHeaderViewForPreviewMaybe();
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		if(featuredFragment==null)
			return;
		if(featuredFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "featured", featuredFragment);
		if(timelineFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "timeline", timelineFragment);
		if(aboutFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "about", aboutFragment);
		if(savedFragment!=null && savedFragment.isAdded())
			getChildFragmentManager().putFragment(outState, "saved", savedFragment);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(contentView!=null){
			if(Build.VERSION.SDK_INT>=29 && insets.getTappableElementInsets().bottom==0){
				int insetBottom=insets.getSystemWindowInsetBottom();
				childInsets=insets.inset(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+insetBottom;
				applyChildWindowInsets();
				insets=insets.inset(0, 0, 0, insetBottom);
			}else{
				((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16);
			}
		}
		super.onApplyWindowInsets(insets);
	}

	private void applyChildWindowInsets(){
		if(timelineFragment!=null && timelineFragment.isAdded() && childInsets!=null){
			timelineFragment.onApplyWindowInsets(childInsets);
			featuredFragment.onApplyWindowInsets(childInsets);
			if(savedFragment!=null)
				savedFragment.onApplyWindowInsets(childInsets);
		}
	}

	private void bindHeaderViewForPreviewMaybe(){
		if(loaded)
			return;
		String username=getArguments().getString("accountUsername");
		String domain=getArguments().getString("accountDomain");
		if(TextUtils.isEmpty(username) || TextUtils.isEmpty(domain))
			return;
		content.setVisibility(View.VISIBLE);
		progress.setVisibility(View.GONE);
		errorView.setVisibility(View.GONE);
		innerProgress.setVisibility(View.VISIBLE);
		this.username.setText(username);
		name.setText(username);
		usernameDomain.setText(domain);
		avatar.setImageResource(R.drawable.image_placeholder);
		cover.setImageResource(R.drawable.image_placeholder);
		actions.setVisibility(View.GONE);
		bio.setVisibility(View.GONE);
		countersLayout.setVisibility(View.GONE);
		tabsDivider.setVisibility(View.GONE);
	}

	private void bindHeaderView(){
		if(innerProgress.getVisibility()==View.VISIBLE){
			TransitionManager.beginDelayedTransition(contentView, new TransitionSet()
					.addTransition(new Fade(Fade.IN | Fade.OUT))
					.excludeChildren(actions, true)
					.setDuration(250)
					.setInterpolator(CubicBezierInterpolator.DEFAULT)
			);
			innerProgress.setVisibility(View.GONE);
			countersLayout.setVisibility(View.VISIBLE);
			actions.setVisibility(View.VISIBLE);
			tabsDivider.setVisibility(View.VISIBLE);
		}
		setTitle(account.displayName);
		setSubtitle(getResources().getQuantityString(R.plurals.x_posts, (int)(account.statusesCount%1000), account.statusesCount));
		ViewImageLoader.loadWithoutAnimation(avatar, avatar.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.avatar : account.avatarStatic, V.dp(100), V.dp(100)));
		ViewImageLoader.loadWithoutAnimation(cover, cover.getDrawable(), new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? account.header : account.headerStatic, 1000, 1000));
		SpannableStringBuilder ssb=new SpannableStringBuilder(account.displayName);
		if(GlobalUserPreferences.customEmojiInNames)
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
		name.setText(ssb);
		setTitle(ssb);

		boolean isSelf=AccountSessionManager.getInstance().isSelf(accountID, account);

		if(account.locked){
			ssb=new SpannableStringBuilder(account.username);
			ssb.append(" ");
			Drawable lock=username.getResources().getDrawable(R.drawable.ic_lock_fill1_20px, getActivity().getTheme()).mutate();
			lock.setBounds(0, 0, lock.getIntrinsicWidth(), lock.getIntrinsicHeight());
			lock.setTint(username.getCurrentTextColor());
			ssb.append(getString(R.string.manually_approves_followers), new ImageSpanThatDoesNotBreakShitForNoGoodReason(lock, ImageSpan.ALIGN_BOTTOM), 0);
			username.setText(ssb);
		}else{
			username.setText(account.username);
		}
		String domain=account.getDomain();
		if(TextUtils.isEmpty(domain))
			domain=AccountSessionManager.get(accountID).domain;
		usernameDomain.setText(domain);

		CharSequence parsedBio=HtmlParser.parse(account.note, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID, account, getActivity());
		if(TextUtils.isEmpty(parsedBio)){
			bio.setVisibility(View.GONE);
		}else{
			bio.setVisibility(View.VISIBLE);
			bio.setText(parsedBio);
		}
		followersCount.setText(UiUtils.abbreviateNumber(account.followersCount));
		followingCount.setText(UiUtils.abbreviateNumber(account.followingCount));
		followersLabel.setText(getResources().getQuantityString(R.plurals.followers, (int)Math.min(999, account.followersCount)));
		followingLabel.setText(getResources().getQuantityString(R.plurals.following, (int)Math.min(999, account.followingCount)));

		UiUtils.loadCustomEmojiInTextView(name);
		UiUtils.loadCustomEmojiInTextView(bio);

		if(AccountSessionManager.getInstance().isSelf(accountID, account)){
			actionButton.setText(R.string.edit_profile);
			TypedArray ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.background});
			actionButton.setBackground(ta.getDrawable(0));
			ta.recycle();
			ta=actionButton.getContext().obtainStyledAttributes(R.style.Widget_Mastodon_M3_Button_Tonal, new int[]{android.R.attr.textColor});
			actionButton.setTextColor(ta.getColorStateList(0));
			ta.recycle();
		}else{
			actionButton.setVisibility(View.GONE);
		}

		fields.clear();

		AccountField joined=new AccountField();
		joined.parsedName=joined.name=getString(R.string.profile_joined);
		joined.parsedValue=joined.value=DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDateTime.ofInstant(account.createdAt, ZoneId.systemDefault()));
		fields.add(joined);

		for(AccountField field:account.fields){
			field.parsedValue=ssb=HtmlParser.parse(field.value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID, account, getActivity());
			field.valueEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			ssb=new SpannableStringBuilder(field.name);
			HtmlParser.parseCustomEmoji(ssb, account.emojis);
			field.parsedName=ssb;
			field.nameEmojis=ssb.getSpans(0, ssb.length(), CustomEmojiSpan.class);
			field.emojiRequests=new ArrayList<>(field.nameEmojis.length+field.valueEmojis.length);
			for(CustomEmojiSpan span:field.nameEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			for(CustomEmojiSpan span:field.valueEmojis){
				field.emojiRequests.add(span.createImageLoaderRequest());
			}
			fields.add(field);
		}

		if(aboutFragment!=null){
			aboutFragment.setFields(fields);
		}
	}

	private void updateToolbar(){
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
		if(onScrollListener!=null){
			onScrollListener.setViews(getToolbar());
		}
		getToolbar().setTranslationZ(tabBarIsAtTop ? 0 : V.dp(3));
	}

	private CharSequence makeRedString(CharSequence s){
		int color=UiUtils.getThemeColor(getActivity(), R.attr.colorM3Error);
		SpannableString ss=new SpannableString(s);
		ss.setSpan(new ForegroundColorSpan(color), 0, ss.length(), 0);
		return ss;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		if(isOwnProfile && isInEditMode){
			editSaveMenuItem=menu.add(0, R.id.save, 0, R.string.save_changes);
			editSaveMenuItem.setIcon(R.drawable.ic_save_24px);
			editSaveMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			editSaveMenuItem.setVisible(!isActionButtonInView());
			return;
		}
		if(relationship==null && !isOwnProfile)
			return;
		inflater.inflate(isOwnProfile ? R.menu.profile_own : R.menu.profile, menu);
		menu.findItem(R.id.share).setTitle(R.string.share_user);
		if(isOwnProfile)
			return;

		menu.findItem(R.id.mute).setTitle(getString(relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getDisplayUsername()));
		menu.findItem(R.id.block).setTitle(makeRedString(getString(relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getDisplayUsername())));
		menu.findItem(R.id.report).setTitle(makeRedString(getString(R.string.report_user, account.getDisplayUsername())));
		if(relationship.following)
			menu.findItem(R.id.hide_boosts).setTitle(getString(relationship.showingReblogs ? R.string.hide_boosts_from_user : R.string.show_boosts_from_user));
		else
			menu.findItem(R.id.hide_boosts).setVisible(false);
		if(!account.isLocal())
			menu.findItem(R.id.block_domain).setTitle(makeRedString(getString(relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain())));
		else
			menu.findItem(R.id.block_domain).setVisible(false);
		menu.findItem(R.id.add_to_list).setVisible(relationship.following);

		if(relationship.following){
			MenuItem notifications=menu.findItem(R.id.notifications);
			notifications.setVisible(true);
			notifications.setIcon(relationship.notifying ? R.drawable.ic_notifications_fill1_24px : R.drawable.ic_notifications_24px);
			notifications.setTitle(getString(relationship.notifying ? R.string.disable_new_post_notifications : R.string.enable_new_post_notifications, account.getDisplayUsername()));
		}

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.P && !UiUtils.isEMUI() && !UiUtils.isMagic()){
			menu.setGroupDividerEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if(id==R.id.share){
			UiUtils.openSystemShareSheet(getActivity(), account);
		}else if(id==R.id.mute){
			confirmToggleMuted();
		}else if(id==R.id.block){
			confirmToggleBlocked();
		}else if(id==R.id.report){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("reportAccount", Parcels.wrap(account));
			args.putParcelable("relationship", Parcels.wrap(relationship));
			Nav.go(getActivity(), ReportReasonChoiceFragment.class, args);
		}else if(id==R.id.open_in_browser){
			UiUtils.launchWebBrowser(getActivity(), account.url);
		}else if(id==R.id.block_domain){
			UiUtils.confirmToggleBlockDomain(getActivity(), accountID, account, relationship.domainBlocking, ()->{
				relationship.domainBlocking=!relationship.domainBlocking;
				updateRelationship();
			}, this::updateRelationship);
		}else if(id==R.id.hide_boosts){
			new SetAccountFollowed(account.id, true, !relationship.showingReblogs, relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.save){
			if(isInEditMode)
				saveAndExitEditMode();
		}else if(id==R.id.add_to_list){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putParcelable("targetAccount", Parcels.wrap(account));
			Nav.go(getActivity(), AddAccountToListsFragment.class, args);
		}else if(id==R.id.notifications){
			new SetAccountFollowed(account.id, true, relationship.showingReblogs, !relationship.notifying)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Relationship result){
							updateRelationship(result);
							new Snackbar.Builder(getActivity())
									.setText(result.notifying ? R.string.new_post_notifications_enabled : R.string.new_post_notifications_disabled)
									.show();
						}

						@Override
						public void onError(ErrorResponse error){
							error.showToast(getActivity());
						}
					})
					.wrapProgress(getActivity(), R.string.loading, false)
					.exec(accountID);
		}else if(id==R.id.copy_link){
			getActivity().getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null, account.url));
			UiUtils.maybeShowTextCopiedToast(getActivity());
		}
		return true;
	}

	private void loadRelationship(){
		MastodonAPIRequest<List<Relationship>> relReq=new GetAccountRelationships(Collections.singletonList(account.id));
		relReq.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Relationship> result){
						relationshipRequests.remove(relReq);
						if(getActivity()==null)
							return;
						if(!result.isEmpty()){
							relationship=result.get(0);
							updateRelationship();
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipRequests.remove(relReq);
					}
				})
				.exec(accountID);
		MastodonAPIRequest<List<FamiliarFollowers>> followersReq=new GetAccountFamiliarFollowers(Set.of(account.id));
		followersReq.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<FamiliarFollowers> result){
						relationshipRequests.remove(followersReq);
						if(getActivity()==null)
							return;
						for(FamiliarFollowers ff:result){
							if(ff.id.equals(account.id)){
								familiarFollowers=ff.accounts;
								updateFamiliarFollowers();
								break;
							}
						}
					}

					@Override
					public void onError(ErrorResponse error){
						relationshipRequests.remove(followersReq);
					}
				})
				.exec(accountID);
		relationshipRequests.add(relReq);
		relationshipRequests.add(followersReq);
	}

	private void updateRelationship(){
		invalidateOptionsMenu();
		actionButton.setVisibility(View.VISIBLE);
		UiUtils.setRelationshipToActionButtonM3(relationship, actionButton);
		actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		followsYouView.setVisibility(relationship.followedBy ? View.VISIBLE : View.GONE);
	}

	private void updateFamiliarFollowers(){
		if(!familiarFollowers.isEmpty()){
			familiarFollowersRow.setVisibility(View.VISIBLE);
			List<AccountViewModel> followers=familiarFollowers.stream().limit(3).map(a->new AccountViewModel(a, accountID, false, getActivity())).collect(Collectors.toList());
			String template=switch(familiarFollowers.size()){
				case 1 -> getString(R.string.familiar_followers_one, "{first}");
				case 2 -> getString(R.string.familiar_followers_two, "{first}", "{second}");
				default -> getResources().getQuantityString(R.plurals.familiar_followers_many, familiarFollowers.size()-2, "{first}", "{second}", familiarFollowers.size()-2);
			};
			SpannableStringBuilder ssb=new SpannableStringBuilder(template);
			if(familiarFollowers.size()>1){
				int index=template.indexOf("{second}");
				ssb.replace(index, index+8, followers.get(1).parsedName);
				template=template.replace("{second}", "#".repeat(followers.get(1).parsedName.length()));
			}
			int index=template.indexOf("{first}");
			ssb.replace(index, index+7, followers.get(0).parsedName);
			familiarFollowersLabel.setText(ssb);
			UiUtils.loadCustomEmojiInTextView(familiarFollowersLabel);
			if(familiarFollowers.size()<3)
				familiarFollowersAvatars[2].setVisibility(View.GONE);
			if(familiarFollowers.size()<2)
				familiarFollowersAvatars[1].setVisibility(View.GONE);

			int i=0;
			for(AccountViewModel avm:followers){
				ViewImageLoader.loadWithoutAnimation(familiarFollowersAvatars[i], getResources().getDrawable(R.drawable.image_placeholder, getActivity().getTheme()), avm.avaRequest);
				i++;
			}
		}
	}

	private void onScrollChanged(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY){
		if(scrollY>cover.getHeight()){
			cover.setTranslationY(scrollY-(cover.getHeight()));
			cover.setTranslationZ(V.dp(10));
			cover.setTransform(cover.getHeight()/2f);
		}else{
			cover.setTranslationY(0f);
			cover.setTranslationZ(0f);
			cover.setTransform(scrollY/2f);
		}
		cover.invalidate();
		if(currentPhotoViewer!=null){
			currentPhotoViewer.offsetView(0, oldScrollY-scrollY);
		}
		onScrollListener.onScrollChange(v, scrollX, scrollY, oldScrollX, oldScrollY);

		boolean newTabBarIsAtTop=!scrollView.canScrollVertically(1);
		if(newTabBarIsAtTop!=tabBarIsAtTop){
			tabBarIsAtTop=newTabBarIsAtTop;

			if(tabBarIsAtTop){
				// ScrollView would sometimes leave 1 pixel unscrolled, force it into the correct scrollY
				int maxY=scrollView.getChildAt(0).getHeight()-scrollView.getHeight();
				if(scrollView.getScrollY()!=maxY)
					scrollView.scrollTo(0, maxY);
			}

			if(tabBarColorAnim!=null)
				tabBarColorAnim.cancel();
			AnimatorSet set=new AnimatorSet();
			set.playTogether(
					ObjectAnimator.ofInt(tabsColorBackground, "alpha", tabBarIsAtTop ? 20 : 0),
					ObjectAnimator.ofFloat(tabbar, View.TRANSLATION_Z, tabBarIsAtTop ? V.dp(3) : 0),
					ObjectAnimator.ofFloat(getToolbar(), View.TRANSLATION_Z, tabBarIsAtTop ? 0 : V.dp(3)),
					ObjectAnimator.ofFloat(tabsDivider, View.ALPHA, tabBarIsAtTop ? 0 : 1)
			);
			set.setDuration(150);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					tabBarColorAnim=null;
				}
			});
			tabBarColorAnim=set;
			set.start();
		}
		if(isInEditMode && editSaveMenuItem!=null){
			boolean buttonInView=isActionButtonInView();
			if(buttonInView==editSaveMenuItem.isVisible()){
				editSaveMenuItem.setVisible(!buttonInView);
			}
		}
		if((scrollY==0 && oldScrollY!=0) || (scrollY!=0 && oldScrollY==0)){
			refreshLayout.setEnabled(scrollY==0);
		}
	}

	private Fragment getFragmentForPage(int page){
		return switch(page){
			case 0 -> featuredFragment;
			case 1 -> timelineFragment;
			case 2 -> aboutFragment;
			case 3 -> savedFragment;
			default -> throw new IllegalStateException();
		};
	}

	private RecyclerView getScrollableRecyclerView(){
		return getFragmentForPage(pager.getCurrentItem()).getView().findViewById(R.id.list);
	}

	private void onActionButtonClick(View v){
		if(isOwnProfile){
			if(!isInEditMode)
				loadAccountInfoAndEnterEditMode();
			else
				saveAndExitEditMode();
		}else{
			UiUtils.performAccountAction(getActivity(), account, accountID, relationship, actionButton, this::setActionProgressVisible, this::updateRelationship);
		}
	}

	private void setActionProgressVisible(boolean visible){
		actionButton.setTextVisible(!visible);
		actionProgress.setVisibility(visible ? View.VISIBLE : View.GONE);
		if(visible)
			actionProgress.setIndeterminateTintList(actionButton.getTextColors());
		actionButton.setClickable(!visible);
	}

	private void loadAccountInfoAndEnterEditMode(){
		if(editModeLoading)
			return;
		editModeLoading=true;
		setActionProgressVisible(true);
		new GetOwnAccount()
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						enterEditMode(result);
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						editModeLoading=false;
						if(getActivity()==null)
							return;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void enterEditMode(Account account){
		if(isInEditMode)
			throw new IllegalStateException();
		isInEditMode=true;
		invalidateOptionsMenu();
		pager.setUserInputEnabled(false);
		actionButton.setText(R.string.save_changes);
		pager.setCurrentItem(2);
		for(int i=0;i<4;i++){
			tabbar.getTabAt(i).view.setEnabled(false);
		}
		Drawable overlay=getResources().getDrawable(R.drawable.edit_avatar_overlay).mutate();
		avatar.setForeground(overlay);

		Toolbar toolbar=getToolbar();
		Drawable close=getToolbarContext().getDrawable(R.drawable.ic_baseline_close_24).mutate();
		close.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
		toolbar.setNavigationIcon(close);
		toolbar.setNavigationContentDescription(R.string.discard);

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		Runnable updater=new Runnable(){
			@Override
			public void run(){
				// setPadding() calls nullLayouts() internally, forcing the text layout to update
				actionButton.setPadding(actionButton.getPaddingLeft(), 1, actionButton.getPaddingRight(), 0);
				actionButton.setPadding(actionButton.getPaddingLeft(), 0, actionButton.getPaddingRight(), 0);
				actionButton.measure(actionButton.getWidth()|View.MeasureSpec.EXACTLY, actionButton.getHeight()|View.MeasureSpec.EXACTLY);
				actionButton.postOnAnimation(this);
			}
		};
		actionButton.postOnAnimation(updater);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
				.addListener(new Transition.TransitionListener(){
					@Override
					public void onTransitionStart(Transition transition){}

					@Override
					public void onTransitionEnd(Transition transition){
						actionButton.removeCallbacks(updater);
					}

					@Override
					public void onTransitionCancel(Transition transition){}

					@Override
					public void onTransitionPause(Transition transition){}

					@Override
					public void onTransitionResume(Transition transition){}
				})
		);

		name.setVisibility(View.INVISIBLE);
		username.setVisibility(View.INVISIBLE);
		bio.setVisibility(View.GONE);
		countersLayout.setVisibility(View.GONE);
		qrCodeButton.setVisibility(View.GONE);
		usernameDomain.setVisibility(View.INVISIBLE);

		nameEditWrap.setVisibility(View.VISIBLE);
		nameEdit.setText(account.displayName);

		bioEditWrap.setVisibility(View.VISIBLE);
		bioEdit.setText(account.source.note);

		aboutFragment.enterEditMode(account.source.fields);
		refreshLayout.setEnabled(false);
		editDirty=false;
		V.setVisibilityAnimated(fab, View.GONE);
		addBackCallback(editModeBackCallback);
	}

	private void exitEditMode(){
		if(savingEdits)
			return;
		if(getActivity()==null)
			return;
		if(!isInEditMode){
			if(BuildConfig.DEBUG)
				throw new IllegalStateException();
			else
				return;
		}
		isInEditMode=false;
		removeBackCallback(editModeBackCallback);

		invalidateOptionsMenu();
		actionButton.setText(R.string.edit_profile);
		for(int i=0;i<4;i++){
			tabbar.getTabAt(i).view.setEnabled(true);
		}
		pager.setUserInputEnabled(true);
		avatar.setForeground(null);

		Toolbar toolbar=getToolbar();
		if(canGoBack()){
			Drawable back=getToolbarContext().getDrawable(me.grishka.appkit.R.drawable.ic_arrow_back).mutate();
			back.setTint(UiUtils.getThemeColor(getToolbarContext(), R.attr.colorM3OnSurfaceVariant));
			toolbar.setNavigationIcon(back);
			toolbar.setNavigationContentDescription(0);
		}else{
			toolbar.setNavigationIcon(null);
		}
		editSaveMenuItem=null;

		ViewGroup parent=contentView.findViewById(R.id.scrollable_content);
		Runnable updater=new Runnable(){
			@Override
			public void run(){
				// setPadding() calls nullLayouts() internally, forcing the text layout to update
				actionButton.setPadding(actionButton.getPaddingLeft(), 1, actionButton.getPaddingRight(), 0);
				actionButton.setPadding(actionButton.getPaddingLeft(), 0, actionButton.getPaddingRight(), 0);
				actionButton.measure(actionButton.getWidth()|View.MeasureSpec.EXACTLY, actionButton.getHeight()|View.MeasureSpec.EXACTLY);
				actionButton.postOnAnimation(this);
			}
		};
		actionButton.postOnAnimation(updater);
		TransitionManager.beginDelayedTransition(parent, new TransitionSet()
				.addTransition(new Fade(Fade.IN | Fade.OUT))
				.addTransition(new ChangeBounds())
				.setDuration(250)
				.setInterpolator(CubicBezierInterpolator.DEFAULT)
				.addListener(new Transition.TransitionListener(){
					@Override
					public void onTransitionStart(Transition transition){}

					@Override
					public void onTransitionEnd(Transition transition){
						actionButton.removeCallbacks(updater);
					}

					@Override
					public void onTransitionCancel(Transition transition){}

					@Override
					public void onTransitionPause(Transition transition){}

					@Override
					public void onTransitionResume(Transition transition){}
				})
		);
		nameEditWrap.setVisibility(View.GONE);
		bioEditWrap.setVisibility(View.GONE);
		name.setVisibility(View.VISIBLE);
		username.setVisibility(View.VISIBLE);
		bio.setVisibility(View.VISIBLE);
		countersLayout.setVisibility(View.VISIBLE);
		refreshLayout.setEnabled(true);
		usernameDomain.setVisibility(View.VISIBLE);
		qrCodeButton.setVisibility(View.VISIBLE);

		bindHeaderView();
		V.setVisibilityAnimated(fab, View.VISIBLE);
	}

	private void saveAndExitEditMode(){
		if(!isInEditMode)
			throw new IllegalStateException();
		setActionProgressVisible(true);
		savingEdits=true;
		new UpdateAccountCredentials(nameEdit.getText().toString(), bioEdit.getText().toString(), editNewAvatar, editNewCover, aboutFragment.getFields())
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Account result){
						savingEdits=false;
						account=result;
						AccountSessionManager.getInstance().updateAccountInfo(accountID, account);
						exitEditMode();
						setActionProgressVisible(false);
					}

					@Override
					public void onError(ErrorResponse error){
						savingEdits=false;
						error.showToast(getActivity());
						setActionProgressVisible(false);
					}
				})
				.exec(accountID);
	}

	private void confirmToggleMuted(){
		UiUtils.confirmToggleMuteUser(getActivity(), accountID, account, relationship.muting, this::updateRelationship);
	}

	private void confirmToggleBlocked(){
		UiUtils.confirmToggleBlockUser(getActivity(), accountID, account, relationship.blocking, this::updateRelationship);
	}

	private void updateRelationship(Relationship r){
		relationship=r;
		updateRelationship();
	}

	private void onEditModeBackCallback(){
		if(savingEdits)
			return;
		if(editDirty || aboutFragment.isEditDirty()){
			new M3AlertDialogBuilder(getActivity())
					.setTitle(R.string.discard_changes)
					.setPositiveButton(R.string.discard, (dlg, btn)->exitEditMode())
					.setNegativeButton(R.string.cancel, null)
					.show();
		}else{
			exitEditMode();
		}
	}

	private List<Attachment> createFakeAttachments(String url, Drawable drawable){
		Attachment att=new Attachment();
		att.type=Attachment.Type.IMAGE;
		att.url=url;
		att.meta=new Attachment.Metadata();
		att.meta.width=drawable.getIntrinsicWidth();
		att.meta.height=drawable.getIntrinsicHeight();
		return Collections.singletonList(att);
	}

	private void onAvatarClick(View v){
		if(account==null)
			return;
		if(isInEditMode){
			startImagePicker(AVATAR_RESULT);
		}else{
			Drawable ava=avatar.getDrawable();
			if(ava==null)
				return;
			int radius=V.dp(25);
			currentPhotoViewer=new PhotoViewer(getActivity(), null, createFakeAttachments(account.avatar, ava), 0,
					null, accountID, new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->currentPhotoViewer=null, ()->ava, null, null));
		}
	}

	private void onCoverClick(View v){
		if(account==null)
			return;
		if(isInEditMode){
			startImagePicker(COVER_RESULT);
		}else{
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable || account.headerStatic.endsWith("/missing.png"))
				return;
			currentPhotoViewer=new PhotoViewer(getActivity(), null, createFakeAttachments(account.header, drawable), 0,
					null, accountID, new SingleImagePhotoViewerListener(cover, cover, null, this, ()->currentPhotoViewer=null, ()->drawable, ()->avatarBorder.setTranslationZ(2), ()->avatarBorder.setTranslationZ(0)));
		}
	}

	private void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if(!AccountSessionManager.getInstance().isSelf(accountID, account)){
			args.putString("prefilledText", '@'+account.acct+' ');
		}
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	private void onFamiliarFollowersClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		args.putInt("count", familiarFollowers.size());
		Nav.go(getActivity(), FamiliarFollowerListFragment.class, args);
	}

	private void startImagePicker(int requestCode){
		Intent intent=UiUtils.getMediaPickerIntent(new String[]{"image/*"}, 1);
		startActivityForResult(intent, requestCode);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode==Activity.RESULT_OK){
			if(requestCode==AVATAR_RESULT){
				if(!isTablet){
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
				int radius=V.dp(25);
				new AvatarCropper(getActivity(), data.getData(), new SingleImagePhotoViewerListener(avatar, avatarBorder, new int[]{radius, radius, radius, radius}, this, ()->{}, null, null, null), (thumbnail, uri)->{
					getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
					avatar.setImageDrawable(thumbnail);
					editNewAvatar=uri;
					editDirty=true;
				}, ()->getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)).show();
			}else if(requestCode==COVER_RESULT){
				editNewCover=data.getData();
				ViewImageLoader.loadWithoutAnimation(cover, null, new UrlImageLoaderRequest(editNewCover, V.dp(1000), V.dp(1000)));
				editDirty=true;
			}
		}
	}

	@Override
	public void scrollToTop(){
		getScrollableRecyclerView().scrollToPosition(0);
		scrollView.smoothScrollTo(0, 0);
	}

	private void onFollowersOrFollowingClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("targetAccount", Parcels.wrap(account));
		Class<? extends Fragment> cls;
		if(v.getId()==R.id.followers_btn)
			cls=FollowerListFragment.class;
		else if(v.getId()==R.id.following_btn)
			cls=FollowingListFragment.class;
		else
			return;
		Nav.go(getActivity(), cls, args);
	}

	private boolean isActionButtonInView(){
		return actionButton.getVisibility()==View.VISIBLE && actionButtonWrap.getTop()+actionButtonWrap.getHeight()>scrollView.getScrollY();
	}

	@Override
	public void onProvideAssistContent(AssistContent content){
		if(account!=null){
			content.setWebUri(Uri.parse(account.url));
		}
	}

	private class ProfilePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=new FrameLayout(parent.getContext());
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){
			Fragment fragment=getFragmentForPage(position);
			FrameLayout fragmentView=tabViews[position];
			fragmentView.setVisibility(View.VISIBLE);
			if(fragmentView.getParent() instanceof ViewGroup parent)
				parent.removeView(fragmentView);
			((FrameLayout)holder.itemView).addView(fragmentView);
			if(!fragment.isAdded()){
				getChildFragmentManager().beginTransaction().add(fragmentView.getId(), fragment).commit();
				holder.itemView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						getChildFragmentManager().executePendingTransactions();
						if(fragment.isAdded()){
							holder.itemView.getViewTreeObserver().removeOnPreDrawListener(this);
							applyChildWindowInsets();
						}
						return true;
					}
				});
			}
		}

		@Override
		public int getItemCount(){
			return loaded ? (isOwnProfile ? 4 : 3) : 0;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
