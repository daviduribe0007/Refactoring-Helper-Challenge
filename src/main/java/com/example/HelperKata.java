package com.example;


import reactor.core.publisher.Flux;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class HelperKata {
    private static final String EMPTY_STRING = "";
    private static String ANTERIOR_BONO = null;
    protected static String bonoEnviado = null;
    protected static String dateValidated = null;
    private static String bonoForObject = null;
    protected static String errorMessage = null;
    private static AtomicInteger counter = new AtomicInteger(0);
    private static Set<String> codes = new HashSet<>();


    public static Flux<CouponDetailDto> getListFromBase64File(final String fileBase64) {
        String characterSeparated = FileCSVEnum.CHARACTER_DEFAULT.getId();

        return createFluxFrom(fileBase64)
                .map(coupon -> optionals(asignCoupon(coupon
                        .split(characterSeparated))));

    }

    private static CouponDetailDto optionals(Coupon coupon) {

        return Optional
                .of(coupon)
                .filter(HelperKata::emptyCoupon)
                .map(couponFiltered ->
                        couponWithFileErrorColumnEmpty()
                )
                .orElseGet(() ->
                   getCouponDefaultDetailDto()
                );

    }

    private static CouponDetailDto getCouponDefaultDetailDto() {
        validateCodeDuplicate();
        return couponDetailDto();
    }

    private static CouponDetailDto couponWithFileErrorColumnEmpty() {
        errorMessage = ExperienceErrorsEnum.FILE_ERROR_COLUMN_EMPTY.toString();
        return couponDetailDto();
    }

    private static void validateCodeDuplicate() {
        if (!codes.add(bonoEnviado)) {
            dateValidated = null;
            errorMessage = ExperienceErrorsEnum.FILE_ERROR_CODE_DUPLICATE.toString();
        }
    }


    private static CouponDetailDto couponDetailDto() {

        return CouponDetailDto.aCouponDetailDto()
                .withCode(getBonoForObject())
                .withDueDate(dateValidated)
                .withNumberLine(counter.incrementAndGet())
                .withMessageError(errorMessage)
                .withTotalLinesFile(1)
                .build();
    }

    private static Boolean emptyCoupon(Coupon coupon) {
        return coupon.getCode().equals(EMPTY_STRING) || coupon.getDate().equals(EMPTY_STRING);
    }

    private static Coupon asignCoupon(String[] data) {
        return new Coupon(data[0], data[1]);
    }

    private static String getBonoForObject() {
        ANTERIOR_BONO = typeBono(bonoEnviado);
        return Boolean.TRUE.equals(bonoEqualsBonoEnviadoBoolean()) ? null : bonoEnviado;
    }

    private static Boolean bonoEqualsBonoEnviadoBoolean() {
        return !ANTERIOR_BONO.equals(typeBono(bonoEnviado));
    }


    public static String typeBono(String bonoIn) {
        if (bonoLengthBoolean(bonoIn)) {
            return ValidateCouponEnum.EAN_13.getTypeOfEnum();
        }
        return bonoWithAstericksBoolean(bonoIn) ? ValidateCouponEnum.EAN_39.getTypeOfEnum()
                : ValidateCouponEnum.ALPHANUMERIC.getTypeOfEnum();

    }

    private static boolean bonoWithAstericksBoolean(String bonoIn) {
        return bonoIn.startsWith("*")
                && validLengthBono(bonoIn, 1, 43);
    }

    private static boolean validLengthBono(String bonoIn, Integer max, Integer min) {
        return bonoIn.replace("*", "").length() >= max
                && bonoIn.replace("*", "").length() <= min;
    }

    private static boolean bonoLengthBoolean(String bonoIn) {
        return bonoIn.chars().allMatch(Character::isDigit)
                && validLengthBono(bonoIn, 12, 13);
    }

    public static boolean validateDateRegex(String dateForValidate) {
        String regex = FileCSVEnum.PATTERN_DATE_DEFAULT.getId();
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dateForValidate);
        return matcher.matches();
    }

    private static byte[] decodeBase64(final String fileBase64) {
        return Base64.getDecoder().decode(fileBase64);

    }

    private static Flux<String> createFluxFrom(String fileBase64) {
        return Flux.using(
                () -> new BufferedReader(new InputStreamReader(
                        new ByteArrayInputStream(decodeBase64(fileBase64))
                )).lines().skip(1),
                Flux::fromStream,
                Stream::close
        );
    }

    public static boolean validateDateIsMinor(String dateForValidate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(FileCSVEnum.PATTERN_SIMPLE_DATE_FORMAT.getId());
            Date dateActual = sdf.parse(sdf.format(new Date()));
            Date dateCompare = sdf.parse(dateForValidate);
            return dateCompare.compareTo(dateActual) <= 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
