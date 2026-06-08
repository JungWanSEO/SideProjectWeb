"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/lib/auth";

/** 상단 네비게이션 — 로그인 상태에 따라 로그인 링크 / 유저+로그아웃 표시. */
export default function Header() {
  const { user, loading, logout } = useAuth();
  const pathname = usePathname();

  // 어드민 경로는 자체 사이드바 레이아웃을 쓰므로 스토어 헤더는 숨긴다.
  if (pathname?.startsWith("/admin")) return null;

  return (
    <header className="border-b border-gray-200">
      <nav className="mx-auto flex max-w-5xl items-center justify-between p-4">
        <Link href="/" className="font-bold">
          commerce
        </Link>
        <div className="flex items-center gap-4 text-sm">
          <Link href="/products" className="hover:underline">
            상품
          </Link>
          {loading ? null : user ? (
            <>
              <Link href="/orders" className="hover:underline">
                주문내역
              </Link>
              <Link href="/cart" className="hover:underline">
                장바구니
              </Link>
              {user.role === "ADMIN" && (
                <Link href="/admin/settlements" className="font-medium text-gray-900 hover:underline">
                  관리자
                </Link>
              )}
              <span className="text-gray-600">{user.nickname}님</span>
              <button onClick={() => logout()} className="text-gray-500 hover:underline">
                로그아웃
              </button>
            </>
          ) : (
            <Link href="/login" className="hover:underline">
              로그인
            </Link>
          )}
        </div>
      </nav>
    </header>
  );
}
