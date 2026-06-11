import Link from "next/link";
import { buttonClass } from "@/components/ui/Button";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-[calc(100vh-65px)] max-w-6xl flex-col items-center justify-center gap-7 px-6 py-16 text-center">
      <span className="text-xs uppercase tracking-[0.35em] text-clay">Fashion Select Shop</span>
      <h1 className="font-serif text-5xl font-extrabold leading-tight text-ink sm:text-6xl">
        오늘의 무드를
        <br />
        고르다, ATELIER
      </h1>
      <p className="max-w-md text-muted">
        엄선한 브랜드의 옷을 한 곳에서. 절제된 셀렉션으로 당신의 하루를 완성하세요.
      </p>
      <Link href="/products" className={buttonClass("primary", "lg", "mt-2")}>
        컬렉션 보기 →
      </Link>
    </main>
  );
}
