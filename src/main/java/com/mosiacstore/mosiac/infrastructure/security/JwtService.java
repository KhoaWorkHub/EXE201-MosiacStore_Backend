package com.mosiacstore.mosiac.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String generateToken(UserDetails userDetails) throws DecoderException {
        Map<String, Object> extraClaims = new HashMap<>();

        if (userDetails instanceof CustomUserDetail) {
            CustomUserDetail customUserDetail = (CustomUserDetail) userDetails;
            extraClaims.put("role", customUserDetail.getUser().getRole().name());
            extraClaims.put("fullName", customUserDetail.getUser().getFullName());
        }

        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) throws DecoderException {
        return buildToken(extraClaims, userDetails, jwtProperties.getExpiration());
    }

    public String generateRefreshToken(UserDetails userDetails) throws DecoderException {
        return buildToken(new HashMap<>(), userDetails, jwtProperties.getRefreshExpiration());
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) throws DecoderException {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) throws DecoderException {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) throws DecoderException {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) throws DecoderException {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) throws DecoderException {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) throws DecoderException {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) throws DecoderException {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        String raw = jwtProperties.getSecretKey();
        try {
            byte[] keyBytes = Hex.decodeHex(raw.toCharArray());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (DecoderException e) {
            throw new IllegalStateException("Invalid hex-encoded JWT secret", e);
        }
    }

}