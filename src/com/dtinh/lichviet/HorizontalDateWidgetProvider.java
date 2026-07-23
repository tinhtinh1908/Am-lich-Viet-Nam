package com.dtinh.lichviet;

/** Top-level 2x1 provider for launcher and HyperOS compatibility. */
public final class HorizontalDateWidgetProvider extends CalendarWidgetProvider {
    @Override protected int layoutId() { return R.layout.calendar_widget; }

    @Override protected Class<? extends CalendarWidgetProvider> providerClass() {
        return HorizontalDateWidgetProvider.class;
    }
}
