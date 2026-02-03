/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp;

import androidx.annotation.Nullable;
import org.fedorahosted.freeotp.utils.Time;

public class Code {
    static class Factory {
        // See: RFC 4226, Section 4, R4
        private static final double RFC_MIN = Math.pow(10, 6);
        private static final double RFC_MAX = Integer.MAX_VALUE;

        static Factory fromIssuer(String issuer) {
            if (issuer == null)
                return new Factory("0123456789");

            if (issuer.equals("Steam"))
                return new Factory("23456789BCDFGHJKMNPQRTVWXY");

            return new Factory("0123456789");
        }

        private final char[] mAlphabet;
        private final int mDigits;

        private Factory(String alphabet) {
            mAlphabet = alphabet.toCharArray();
            mDigits = getDigitsMin();
        }

        private Factory(String alphabet, int defaultDigits) {
            mAlphabet = alphabet.toCharArray();
            mDigits = defaultDigits;
        }

        int getDigitsDefault() {
            return mDigits;
        }

        int getDigitsMin() {
            return (int) Math.ceil(Math.log(RFC_MIN) / Math.log(mAlphabet.length));
        }

        int getDigitsMax() {
            return (int) Math.floor(Math.log(RFC_MAX) / Math.log(mAlphabet.length));
        }

        Code makeHotpCode(int code, @Nullable Integer digits, int period) {
            return makeCode(code, digits, period, false);
        }

        Code makeTotpCode(int code, @Nullable Integer digits, int period) {
            return makeCode(code, digits, period, true);
        }

        private Code makeCode(int code, @Nullable Integer digits, int period, boolean alignToWindow) {
            if (digits == null)
                digits = mDigits;

            char[] buffer = new char[digits];

            for (int i = 0; i < digits; i++) {
                buffer[digits - i - 1] = mAlphabet[code % mAlphabet.length];
                code /= mAlphabet.length;
            }

            return new Code(new String(buffer), period, alignToWindow);
        }
    }

    private final String mCode;
    private final long mPeriod;
    private final long mStart;

    public Code(String code, long period) {
        this(code, period, false);
    }

    public Code(String code, long period, boolean alignToWindow) {
        mPeriod = period * 1000;
        long now = Time.INSTANCE.current();
        if (alignToWindow) {
            // For TOTP: mStart is current window start
            mStart = (now / mPeriod) * mPeriod;
        } else {
            // For HOTP: use current time when code was generated
            mStart = Time.INSTANCE.current();
        }
        mCode = code;
    }

    public String getCode() {
        return mCode;
    }

    public long timeValid() {
        return mPeriod;
    }

    public long timeLeft() {
        long left = mStart + mPeriod - Time.INSTANCE.current();
        return left < 0 ? 0 : left;
    }

    public boolean isValid() {
        return timeLeft() > 0;
    }
}
