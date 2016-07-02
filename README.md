[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-RecyclerViewSwipeDismiss-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1838)
![BSD](http://img.shields.io/badge/license-BSD-green.svg)
[![Jitpack](https://img.shields.io/github/release/CodeFalling/RecyclerViewSwipeDismiss.svg?label=JitPack%20Maven)](https://jitpack.io/#CodeFalling/RecyclerViewSwipeDismiss/)
[![Build Status](https://travis-ci.org/CodeFalling/RecyclerViewSwipeDismiss.svg?branch=master)](https://travis-ci.org/CodeFalling/RecyclerViewSwipeDismiss)
# RecyclerViewSwipeDismiss
A very easy-to-use and non-intrusive implement of Swipe to dismiss for RecyclerView.

## Preview

![preview](RecyclerViewSwipeDismiss.gif)


## How to use

- Add these lines to your `build.gradle`

```gradle
repositories {
	maven {
	    url "https://jitpack.io"
	}
}

dependencies {
	 compile 'com.github.CodeFalling:RecyclerViewSwipeDismiss:v1.1.3'
}
```

- Build `onTouchListener` and bind it to your `RecyclerView`

```java

SwipeDismissRecyclerViewTouchListener listener = new SwipeDismissRecyclerViewTouchListener.Builder(
        recyclerView,
        new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(int position) {
                return true;
            }

            @Override
            public void onDismiss(View view) {
                // Do what you want when dismiss
                
            }
        })
        .setIsVertical(false)
        .setItemTouchCallback(
                new SwipeDismissRecyclerViewTouchListener.OnItemTouchCallBack() {
                    @Override
                    public void onTouch(int index) {
                    	// Do what you want when item be touched
                    }
                })
        .setItemClickCallback(new SwipeDismissRecyclerViewTouchListener.OnItemClickCallBack() {
                    @Override
                    public void onClick(int position) {
                        // Do what you want when item be clicked                    }
                })
        .setBackgroundId(R.drawable.bg_item_normal, R.drawable.bg_item_selected)
        .create();
recyclerView.setOnTouchListener(listener);
```

## More

- `setIsVertical(false)` means allow **swipe in horizontal direction** 

- `listener.setEnabled(false)` can disable swipe to dismiss

- `onTouch` will be called when MOUSE_UP on item without swipe

- `onClick` will be called when ACTION_UP on item within 1 second and move no more than a fixed distance

- By use `setBackgroundId`, you can set background id for item's normal and pressed state, just like the normal effect in RecyclerView

## Sample

You can see sample code in [`sample/MainActivity.java`](https://github.com/CodeFalling/RecyclerViewSwipeDismiss/blob/master/app%2Fsrc%2Fmain%2Fjava%2Fio%2Fgithub%2Fcodefalling%2Frecyclerviewswipedismiss%2Fsample%2FMainActivity.java)
